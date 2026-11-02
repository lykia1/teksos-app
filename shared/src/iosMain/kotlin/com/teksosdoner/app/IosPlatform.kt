@file:OptIn(ExperimentalForeignApi::class)

package com.teksosdoner.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.viewinterop.UIKitView
import com.teksosdoner.app.ui.Ekran
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import platform.AudioToolbox.AudioServicesPlaySystemSound
import platform.AudioToolbox.kSystemSoundID_Vibrate
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.Foundation.NSDate
import platform.Foundation.dataWithContentsOfFile
import platform.posix.memcpy
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import androidx.compose.foundation.Image
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSURL
import platform.Foundation.NSUserDefaults
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UIKit.UIPasteboard
import platform.UIKit.UIViewController
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.WebKit.WKWebsiteDataStore
import platform.WebKit.WKWebsiteDataTypeCookies

/**
 * Swift tarafının (iosApp) sağladığı işler: OneSignal bildirimleri, arka plan konumu, AirPrint.
 * Swift'te `class Kopru: IosKopru` olarak yazılır ve MainViewController'a verilir.
 */
interface IosKopru {
    fun surum(): String
    fun bildirimGiris(kimlik: String)
    fun bildirimCikis()
    fun bildirimIzniIste(ayaraGotur: Boolean)
    fun bildirimAcik(): Boolean
    fun konumCalisiyor(): Boolean
    fun konumIzniVar(): Boolean
    fun konumIzniIste(sonuc: (Boolean) -> Unit)
    fun konumBaslat(url: String, kod: String): Boolean
    fun konumDurdur()
    fun yazdir(html: String, ad: String)
}

private class IosDepo : Depo {
    private val d = NSUserDefaults.standardUserDefaults
    override fun metin(k: String): String? = d.stringForKey("teksos_$k")
    override fun metinYaz(k: String, v: String?) = if (v == null) d.removeObjectForKey("teksos_$k") else d.setObject(v, forKey = "teksos_$k")
    override fun sayi(k: String): Long = d.integerForKey("teksos_$k")
    override fun sayiYaz(k: String, v: Long) = d.setInteger(v, forKey = "teksos_$k")
}

class IosPlatform(private val kopru: IosKopru) : Platform {
    private val kapsam = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    override val ad = "iOS"
    override val surum: String get() = kopru.surum()
    override val depo: Depo = IosDepo()

    private fun ustEkran(): UIViewController? {
        var vc = UIApplication.sharedApplication.keyWindow?.rootViewController
        while (vc?.presentedViewController != null) vc = vc.presentedViewController
        return vc
    }

    override fun ac(url: String) {
        val u = NSURL.URLWithString(url) ?: return
        UIApplication.sharedApplication.openURL(u, emptyMap<Any?, Any?>(), null)
    }

    /** iPhone'da yol tarifi Apple Haritalar'da açılır (Google Haritalar yüklüyse oradan da seçilebilir). */
    override fun haritaAc(hedef: String) = ac("https://maps.apple.com/?dirflg=d&daddr=" + urlKodla(hedef))

    override fun paylas(metin: String) {
        val vc = UIActivityViewController(listOf(metin), null)
        ustEkran()?.let { ust ->
            vc.popoverPresentationController?.sourceView = ust.view
            vc.popoverPresentationController?.sourceRect = CGRectMake(0.0, 0.0, 1.0, 1.0)
            ust.presentViewController(vc, true, null)
        }
    }

    override fun kopyala(metin: String) {
        UIPasteboard.generalPasteboard.string = metin
    }

    override fun titret() = AudioServicesPlaySystemSound(kSystemSoundID_Vibrate)

    override fun fisYazdir(siparisId: Int, mutfak: Boolean) {
        kapsam.launch {
            runCatching {
                val sube = Oturum.sube?.let { "&_sube=$it" } ?: ""
                val html = Api.http.get(Api.sunucu + "kasa/fis.php?id=" + siparisId + (if (mutfak) "&kitchen=1" else "") + sube) {
                    header("User-Agent", Api.UA)
                    Oturum.anahtar?.let { header("Cookie", "teksos_app=$it") }
                }.bodyAsText()
                kopru.yazdir(html, (if (mutfak) "Mutfak fişi #" else "Adisyon #") + siparisId)
            }
        }
    }

    override fun cerezTemizle() {
        WKWebsiteDataStore.defaultDataStore().removeDataOfTypes(
            setOf(WKWebsiteDataTypeCookies), NSDate.dateWithTimeIntervalSince1970(0.0)
        ) {}
    }

    override fun bildirimGiris(kimlik: String) = kopru.bildirimGiris(kimlik)
    override fun bildirimCikis() = kopru.bildirimCikis()
    override fun bildirimIzniIste(ayaraGotur: Boolean) = kopru.bildirimIzniIste(ayaraGotur)
    override fun bildirimAcik(): Boolean = kopru.bildirimAcik()

    override fun konumCalisiyor(): Boolean = kopru.konumCalisiyor()
    override fun konumIzniVar(): Boolean = kopru.konumIzniVar()

    @Composable
    override fun konumIzniIsteyici(sonuc: (Boolean) -> Unit): () -> Unit {
        val guncel by rememberUpdatedState(sonuc)
        return remember { { kopru.konumIzniIste { verildi -> guncel(verildi) } } }
    }

    override fun konumBaslat(url: String, kod: String): Boolean = kopru.konumBaslat(url, kod)
    override fun konumDurdur() = kopru.konumDurdur()

    override val pilAyariVar = false
    override fun pilSerbest() = true
    override fun pilIzniIste() {}
    override fun ayarlariAc() = ac(UIApplicationOpenSettingsURLString)

    override fun resimCoz(bayt: ByteArray): ImageBitmap? =
        runCatching { org.jetbrains.skia.Image.makeFromEncoded(bayt).toComposeImageBitmap() }.getOrNull()

    private val logoResmi: ImageBitmap? by lazy {
        val yol = NSBundle.mainBundle.pathForResource("logo", "png") ?: return@lazy null
        val veri = NSData.dataWithContentsOfFile(yol) ?: return@lazy null
        val bayt = ByteArray(veri.length.toInt())
        bayt.usePinned { memcpy(it.addressOf(0), veri.bytes, veri.length) }
        resimCoz(bayt)
    }

    @Composable
    override fun Logo(modifier: Modifier) {
        logoResmi?.let { Image(it, "Teksos", modifier) }
    }

    @Composable
    override fun WebEkrani(url: String, baslik: String, geri: (() -> Unit)?) {
        val tamUrl = remember(url) {
            val s = Oturum.sube
            if (s.isNullOrEmpty()) url else url + (if (url.contains('?')) "&" else "?") + "_sube=" + s
        }
        Ekran(baslik, geri = geri) {
            UIKitView(
                factory = {
                    val ayar = WKWebViewConfiguration()
                    ayar.applicationNameForUserAgent = "TeksosApp/3.0 TeksosNative iOS"
                    val w = WKWebView(frame = CGRectMake(0.0, 0.0, 1.0, 1.0), configuration = ayar)
                    w.allowsBackForwardNavigationGestures = true
                    NSURL.URLWithString(tamUrl)?.let { u ->
                        val istek = NSMutableURLRequest(uRL = u)
                        Oturum.anahtar?.let { istek.setValue("teksos_app=$it", forHTTPHeaderField = "Cookie") }
                        w.loadRequest(istek)
                    }
                    w
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

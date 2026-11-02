package com.teksosdoner.app

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.webkit.CookieManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.foundation.Image
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.app.NotificationManagerCompat
import com.onesignal.OneSignal
import java.lang.ref.WeakReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private class AndroidDepo(c: Context) : Depo {
    private val p = c.getSharedPreferences("teksos", Context.MODE_PRIVATE)
    override fun metin(k: String): String? = p.getString(k, null)
    override fun metinYaz(k: String, v: String?) {
        p.edit().apply { if (v == null) remove(k) else putString(k, v) }.apply()
    }
    // Eski sürümlerde bazı değerler Long/Boolean olarak saklandı
    override fun sayi(k: String): Long = runCatching { p.getLong(k, 0L) }.getOrElse { runCatching { if (p.getBoolean(k, false)) 1L else 0L }.getOrDefault(0L) }
    override fun sayiYaz(k: String, v: Long) { p.edit().remove(k).putLong(k, v).apply() }
}

class AndroidPlatform(private val app: Context) : Platform {
    /** Ekrandaki etkinlik (yazdırma vb. için) */
    var etkinlik: WeakReference<Activity>? = null
    private val kapsam = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override val ad = "Android"
    override val surum: String = runCatching {
        val p = app.packageManager.getPackageInfo(app.packageName, 0)
        "${p.versionName} (${if (Build.VERSION.SDK_INT >= 28) p.longVersionCode else @Suppress("DEPRECATION") p.versionCode.toLong()})"
    }.getOrDefault("?")
    override val depo: Depo = AndroidDepo(app)

    private fun baslat(i: Intent) {
        val a = etkinlik?.get()
        runCatching { if (a != null) a.startActivity(i) else app.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    override fun ac(url: String) {
        val u = Uri.parse(url)
        baslat(if (u.scheme == "tel") Intent(Intent.ACTION_DIAL, u) else Intent(Intent.ACTION_VIEW, u))
    }

    override fun paylas(metin: String) =
        baslat(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, metin), "Paylaş"))

    override fun kopyala(metin: String) {
        (app.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Teksos", metin))
    }

    override fun titret() {
        runCatching {
            app.getSystemService(Vibrator::class.java)?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 250, 120, 250), -1))
        }
    }

    override fun fisYazdir(siparisId: Int, mutfak: Boolean) {
        Yazdir.fis(etkinlik?.get() ?: app, siparisId, mutfak)
    }

    override fun cerezTemizle() {
        runCatching { CookieManager.getInstance().removeAllCookies(null) }
    }

    override fun bildirimGiris(kimlik: String) { OneSignal.login(kimlik) }
    override fun bildirimCikis() { OneSignal.logout() }
    override fun bildirimIzniIste(ayaraGotur: Boolean) {
        kapsam.launch { runCatching { OneSignal.Notifications.requestPermission(ayaraGotur) } }
    }
    override fun bildirimAcik(): Boolean = NotificationManagerCompat.from(app).areNotificationsEnabled()

    override fun konumCalisiyor(): Boolean = Konum.calisiyor()
    override fun konumIzniVar(): Boolean = Konum.izinVar(app)

    @Composable
    override fun konumIzniIsteyici(sonuc: (Boolean) -> Unit): () -> Unit {
        val guncel by rememberUpdatedState(sonuc)
        val l = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { s ->
            guncel(s[Manifest.permission.ACCESS_FINE_LOCATION] == true || s[Manifest.permission.ACCESS_COARSE_LOCATION] == true)
        }
        return {
            val izinler = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= 33) izinler += Manifest.permission.POST_NOTIFICATIONS
            l.launch(izinler.toTypedArray())
        }
    }

    override fun konumBaslat(url: String, kod: String): Boolean = Konum.baslat(app, url, kod)
    override fun konumDurdur() = Konum.durdur(app)

    override val pilAyariVar = true
    override fun pilSerbest(): Boolean = Konum.pilSerbest(app)
    override fun pilIzniIste() = Konum.pilIzniIste(etkinlik?.get() ?: app)
    override fun ayarlariAc() =
        baslat(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + app.packageName)))

    override fun resimCoz(bayt: ByteArray): ImageBitmap? {
        val sinir = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bayt, 0, bayt.size, sinir)
        var oran = 1
        while (sinir.outWidth / oran > 1600) oran *= 2
        return BitmapFactory.decodeByteArray(bayt, 0, bayt.size, BitmapFactory.Options().apply { inSampleSize = oran })?.asImageBitmap()
    }

    @Composable
    override fun Logo(modifier: Modifier) =
        Image(painterResource(R.drawable.logo), contentDescription = "Teksos", modifier = modifier)

    @Composable
    override fun WebEkrani(url: String, baslik: String, geri: (() -> Unit)?) = com.teksosdoner.app.ui.WebEkrani(url, baslik, geri)
}

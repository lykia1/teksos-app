package com.teksosdoner.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Telefona özgü işler. Android'de MainActivity/TeksosUygulama, iPhone'da MainViewController doldurur.
 * Ekranlar yalnızca bu arayüzü kullanır; böylece aynı ekran kodu iki telefonda da çalışır.
 */
interface Platform {
    /** "Android" ya da "iOS" */
    val ad: String
    /** Uygulama sürümü, ör. "3.2.0 (24)" */
    val surum: String
    /** Kalıcı küçük ayarlar */
    val depo: Depo

    /** tel:, https:, harita, WhatsApp bağlantısı açar */
    fun ac(url: String)
    fun telefonAra(tel: String) = tel.filter { it.isDigit() || it == '+' }.let { if (it.isNotEmpty()) ac("tel:$it") }
    fun haritaAc(hedef: String) = ac("https://www.google.com/maps/dir/?api=1&travelmode=driving&destination=" + urlKodla(hedef))
    fun paylas(metin: String)
    fun kopyala(metin: String)
    fun titret()

    /** Adisyon / mutfak fişini telefonun yazdırma sistemiyle basar */
    fun fisYazdir(siparisId: Int, mutfak: Boolean)
    /** Çıkışta web oturum çerezlerini siler */
    fun cerezTemizle()

    /* Bildirimler */
    fun bildirimGiris(kimlik: String)
    fun bildirimCikis()
    fun bildirimIzniIste(ayaraGotur: Boolean)
    fun bildirimAcik(): Boolean

    /* Kurye konumu */
    fun konumCalisiyor(): Boolean
    fun konumIzniVar(): Boolean
    /** İzin penceresini açan işlev (sonuç: izin verildi mi) */
    @Composable
    fun konumIzniIsteyici(sonuc: (Boolean) -> Unit): () -> Unit
    fun konumBaslat(url: String, kod: String): Boolean
    fun konumDurdur()
    /** Android pil kısıtlaması (iPhone'da yok) */
    val pilAyariVar: Boolean
    fun pilSerbest(): Boolean
    fun pilIzniIste()
    /** Uygulamanın telefon ayarları sayfası */
    fun ayarlariAc()

    /** Kamerayla QR okutur; iptal edilirse null */
    fun qrOku(sonuc: (String?) -> Unit)
    /** Fotoğraf çeker; base64 JPEG ya da iptalde null. onKamera: ön (selfie) kamera tercih edilir */
    fun fotoCek(onKamera: Boolean, sonuc: (String?) -> Unit)
    /** Anlık konum "enlem,boylam"; izin/konum yoksa null */
    fun konumAl(sonuc: (String?) -> Unit)

    /** İndirilen resmi çözer */
    fun resimCoz(bayt: ByteArray): ImageBitmap?

    /** Giriş ekranındaki logo */
    @Composable
    fun Logo(modifier: Modifier)

    /** Uygulama içinde web sayfası (yalnızca TV önizlemesi ve bildirim bağlantıları için) */
    @Composable
    fun WebEkrani(url: String, baslik: String, geri: (() -> Unit)?)
}

/** Kalıcı küçük ayarlar (Android: SharedPreferences, iPhone: NSUserDefaults) */
interface Depo {
    fun metin(k: String): String?
    fun metinYaz(k: String, v: String?)
    fun sayi(k: String): Long
    fun sayiYaz(k: String, v: Long)
}

lateinit var os: Platform

/** Bildirime dokunulunca açılacak sayfa (ör. kurye.php → Paketler ekranı). */
object Bildirim {
    val bekleyen = MutableStateFlow<String?>(null)
}

/** Sunucu ve bildirim ayarları (iki telefonda aynı) */
object Ayar {
    const val SUNUCU = "https://teksosdonerbesevler.com/"
    const val ONESIGNAL_ID = "7fb5f933-2168-419a-8fe8-c4b9cfad13b4"
}

package com.teksosdoner.app

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.pow

/*
 * Android'e özgü yardımcıların (String.format, Locale, SimpleDateFormat) her iki telefonda
 * da çalışan karşılıkları.
 */

/** Yerel ayar: yalnızca ondalık ayracı ve büyük/küçük harf kuralları için. */
class Yerel(val ondalik: Char, val binlik: Char, val turkce: Boolean)

val TR = Yerel(',', '.', true)
val US = Yerel('.', ',', false)

/** Saat/tarih: telefonun yerel saatiyle "yyyy-MM-dd HH:mm:ss". */
expect fun yerelZaman(): String

/** 1970'ten beri geçen milisaniye. */
expect fun simdiMs(): Long

/* ---------------------------- sayı biçimleri ---------------------------- */

/** HALF_UP yuvarlama ile sabit basamaklı yazım (Java'nın %.Nf'si gibi). */
fun ondalikYaz(d: Double, basamak: Int, y: Yerel = TR, binlikli: Boolean = false): String {
    if (d.isNaN()) return "NaN"
    if (d.isInfinite()) return if (d > 0) "∞" else "-∞"
    val carpan = 10.0.pow(basamak)
    val yuvarlanmis = floor(abs(d) * carpan + 0.5 + 1e-9)
    val tam = floor(yuvarlanmis / carpan).toLong()
    val kesir = (yuvarlanmis - tam * carpan).toLong()
    var tamYazi = tam.toString()
    if (binlikli && tamYazi.length > 3) {
        tamYazi = tamYazi.reversed().chunked(3).joinToString(y.binlik.toString()).reversed()
    }
    val negatif = d < 0 && yuvarlanmis > 0
    val sb = StringBuilder()
    if (negatif) sb.append('-')
    sb.append(tamYazi)
    if (basamak > 0) sb.append(y.ondalik).append(kesir.toString().padStart(basamak, '0'))
    return sb.toString()
}

/** String.format(TR, "%.2f", x) gibi çağrılar için (yalnızca %d, %s, %f ve genişlik/sıfır dolgusu). */
fun String.Companion.format(y: Yerel, desen: String, vararg arg: Any?): String {
    val sb = StringBuilder()
    var i = 0
    var n = 0
    while (i < desen.length) {
        val c = desen[i]
        if (c != '%') { sb.append(c); i++; continue }
        i++
        if (i < desen.length && desen[i] == '%') { sb.append('%'); i++; continue }
        var sifir = false
        var virgul = false
        while (i < desen.length && (desen[i] == '0' || desen[i] == ',')) {
            if (desen[i] == '0') sifir = true else virgul = true
            i++
        }
        var genislik = 0
        while (i < desen.length && desen[i].isDigit()) { genislik = genislik * 10 + (desen[i] - '0'); i++ }
        var hassas = -1
        if (i < desen.length && desen[i] == '.') {
            i++; hassas = 0
            while (i < desen.length && desen[i].isDigit()) { hassas = hassas * 10 + (desen[i] - '0'); i++ }
        }
        val tur = if (i < desen.length) desen[i++] else 's'
        val a = arg.getOrNull(n++)
        var yazi = when (tur) {
            'd' -> {
                val l = (a as? Number)?.toLong() ?: 0L
                if (virgul) ondalikYaz(l.toDouble(), 0, y, true) else l.toString()
            }
            'f' -> ondalikYaz((a as? Number)?.toDouble() ?: 0.0, if (hassas < 0) 6 else hassas, y, virgul)
            else -> a.toString()
        }
        if (yazi.length < genislik) {
            yazi = if (sifir && tur != 's') {
                if (yazi.startsWith("-")) "-" + yazi.drop(1).padStart(genislik - 1, '0') else yazi.padStart(genislik, '0')
            } else yazi.padStart(genislik, ' ')
        }
        sb.append(yazi)
    }
    return sb.toString()
}

/* ---------------------------- Türkçe harfler ---------------------------- */

fun String.uppercase(y: Yerel): String =
    if (y.turkce) replace('i', 'İ').replace('ı', 'I').uppercase() else uppercase()

fun String.lowercase(y: Yerel): String =
    if (y.turkce) replace('I', 'ı').replace('İ', 'i').lowercase() else lowercase()

fun Char.titlecase(y: Yerel): String = toString().uppercase(y)

/* ---------------------------- tarih ---------------------------- */

private val GunKisa = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")
private val AyAd = listOf("Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran", "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık")

fun ayAdi(ay: Int): String = AyAd.getOrElse(ay - 1) { "" }

fun ayGunSayisi(yil: Int, ay: Int): Int = when (ay) {
    2 -> if ((yil % 4 == 0 && yil % 100 != 0) || yil % 400 == 0) 29 else 28
    4, 6, 9, 11 -> 30
    else -> 31
}

/** 0 = Pazartesi … 6 = Pazar */
fun haftaninGunu(yil: Int, ay: Int, gun: Int): Int {
    val t = intArrayOf(0, 3, 2, 5, 0, 3, 5, 1, 4, 6, 2, 4)
    val yy = if (ay < 3) yil - 1 else yil
    val pazar0 = (yy + yy / 4 - yy / 100 + yy / 400 + t[ay - 1] + gun) % 7 // 0 = Pazar
    return (pazar0 + 6) % 7
}

/** "2026-11-01" → (2026, 11, 1) */
fun tarihParcala(t: String): Triple<Int, Int, Int>? = runCatching {
    Triple(t.substring(0, 4).toInt(), t.substring(5, 7).toInt(), t.substring(8, 10).toInt())
}.getOrNull()

fun tarihYaz(yil: Int, ay: Int, gun: Int): String =
    yil.toString().padStart(4, '0') + "-" + ay.toString().padStart(2, '0') + "-" + gun.toString().padStart(2, '0')

/** Güne gün ekler / çıkarır. */
fun tarihEkle(t: String, gunSayisi: Int): String {
    var (y, a, g) = tarihParcala(t) ?: return t
    var n = gunSayisi
    while (n > 0) { g++; if (g > ayGunSayisi(y, a)) { g = 1; a++; if (a > 12) { a = 1; y++ } }; n-- }
    while (n < 0) { g--; if (g < 1) { a--; if (a < 1) { a = 12; y-- }; g = ayGunSayisi(y, a) }; n++ }
    return tarihYaz(y, a, g)
}

/** "2026-11" + n ay */
fun ayEkle(ay: String, n: Int): String {
    val y = ay.take(4).toIntOrNull() ?: return ay
    val a = ay.drop(5).take(2).toIntOrNull() ?: return ay
    val toplam = y * 12 + (a - 1) + n
    return (toplam / 12).toString().padStart(4, '0') + "-" + (toplam % 12 + 1).toString().padStart(2, '0')
}

fun gunKisaAdi(t: String): String? = tarihParcala(t)?.let { (y, a, g) -> GunKisa[haftaninGunu(y, a, g)] }

fun bugunMetin(): String = yerelZaman().take(10)
fun buAyMetin(): String = yerelZaman().take(7)

/** Telefona özel rastgele kimlik (32 onaltılık karakter). */
fun rastgeleKimlik(): String = (1..32).joinToString("") { kotlin.random.Random.nextInt(16).toString(16) }

/** URL parametresi kodlama (Uri.encode karşılığı). */
fun urlKodla(s: String): String {
    val sb = StringBuilder()
    for (b in s.encodeToByteArray()) {
        val c = (b.toInt() and 0xFF)
        val ch = c.toChar()
        if (ch.isLetterOrDigit() && c < 128 || ch in "-_.~") sb.append(ch)
        else sb.append('%').append(c.toString(16).uppercase().padStart(2, '0'))
    }
    return sb.toString()
}

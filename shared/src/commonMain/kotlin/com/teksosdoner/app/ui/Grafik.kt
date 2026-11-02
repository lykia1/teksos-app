package com.teksosdoner.app.ui

import com.teksosdoner.app.*

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teksosdoner.app.kisaTl
import com.teksosdoner.app.tl

/** Grafik renk paleti (kanal, kategori, ödeme dilimleri). */
val Palet = listOf(
    Color(0xFFE8590C), Color(0xFF1D4ED8), Color(0xFF15803D), Color(0xFF7C3AED), Color(0xFFDB2777),
    Color(0xFF0E7490), Color(0xFFCA8A04), Color(0xFF57534E), Color(0xFF9333EA), Color(0xFF65A30D)
)

fun renkCoz(hex: String?, yedek: Color): Color = runCatching {
    val h = hex.orEmpty().removePrefix("#")
    if (h.length == 6) Color(("FF$h").toLong(16)) else yedek
}.getOrDefault(yedek)

/** Alanlı çizgi grafik: günlük ciro gibi zaman serileri. Sol üstte en yüksek değer, kesikli ızgara. */
@Composable
fun CizgiGrafik(degerler: List<Double>, etiketler: List<String>, yukseklik: Dp = 150.dp, renk: Color = Turuncu) {
    if (degerler.isEmpty()) return
    val enBuyuk = (degerler.maxOrNull() ?: 0.0).let { if (it <= 0) 1.0 else it }
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            Text2(kisaTl(enBuyuk), Soluk, 10.sp)
            Spacer(Modifier.weight(1f))
            Text2("ort. " + kisaTl(degerler.average()), Soluk, 10.sp)
        }
        Canvas(Modifier.fillMaxWidth().height(yukseklik).padding(top = 4.dp)) {
            val w = size.width
            val h = size.height
            val kesik = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
            for (i in 0..3) {
                val y = h * i / 3f
                drawLine(Cizgi, Offset(0f, y), Offset(w, y), strokeWidth = 1.5f, pathEffect = kesik)
            }
            val ort = (degerler.average() / enBuyuk * h).toFloat()
            drawLine(Soluk.copy(alpha = .5f), Offset(0f, h - ort), Offset(w, h - ort), strokeWidth = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f)))
            val n = degerler.size
            val dx = if (n > 1) w / (n - 1) else w
            val noktalar = degerler.mapIndexed { i, v -> Offset(if (n > 1) i * dx else w / 2, h - (v / enBuyuk * h).toFloat()) }
            val cizgi = Path().apply {
                noktalar.forEachIndexed { i, p ->
                    if (i == 0) moveTo(p.x, p.y) else {
                        val o = noktalar[i - 1]
                        val mx = (o.x + p.x) / 2
                        cubicTo(mx, o.y, mx, p.y, p.x, p.y)
                    }
                }
            }
            val alan = Path().apply {
                addPath(cizgi)
                lineTo(noktalar.last().x, h)
                lineTo(noktalar.first().x, h)
                close()
            }
            drawPath(alan, Brush.verticalGradient(listOf(renk.copy(alpha = .28f), renk.copy(alpha = 0f))))
            drawPath(cizgi, renk, style = Stroke(width = 5f, cap = StrokeCap.Round))
            val enIyi = degerler.indices.maxByOrNull { degerler[it] } ?: 0
            if (n <= 40) noktalar.forEachIndexed { i, p ->
                drawCircle(Color.White, radius = if (i == enIyi) 9f else 5f, center = p)
                drawCircle(renk, radius = if (i == enIyi) 6f else 3.2f, center = p)
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) {
            etiketler.forEach { Text2(it, Soluk, 10.sp, Modifier.weight(1f), TextAlign.Center) }
        }
    }
}

/** Değer etiketli dikey çubuk grafik; karsi: arkada gri karşılaştırma. */
@Composable
fun CubukGrafik(
    degerler: List<Double>, etiketler: List<String>, karsi: List<Double>? = null, yukseklik: Dp = 130.dp,
    vurgu: Int? = null, degerYaz: Boolean = true,
) {
    if (degerler.isEmpty()) return
    val enBuyuk = ((degerler + (karsi ?: emptyList())).maxOrNull() ?: 0.0).let { if (it <= 0) 1.0 else it }
    val enIyi = vurgu ?: degerler.indices.maxByOrNull { degerler[it] }
    Column(Modifier.fillMaxWidth()) {
        if (degerYaz) Row(Modifier.fillMaxWidth()) {
            degerler.forEachIndexed { i, v -> Text2(kisaTl(v), if (i == enIyi) Turuncu else Soluk, 9.sp, Modifier.weight(1f), TextAlign.Center, kalin = i == enIyi) }
        }
        Canvas(Modifier.fillMaxWidth().height(yukseklik).padding(top = 2.dp)) {
            val n = degerler.size
            val aralik = size.width / n
            val gen = aralik * 0.62f
            drawLine(Cizgi, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 2f)
            degerler.forEachIndexed { i, v ->
                val x = i * aralik + (aralik - gen) / 2f
                karsi?.getOrNull(i)?.let { k ->
                    val kh = (k / enBuyuk * size.height).toFloat()
                    if (kh > 0f) drawRoundRect(Color(0xFFE7E5E4), Offset(x, size.height - kh), Size(gen, kh), CornerRadius(6f, 6f))
                }
                val bh = (v / enBuyuk * size.height).toFloat().let { if (v > 0) it.coerceAtLeast(4f) else 0f }
                val ic = if (karsi != null) gen * 0.2f else 0f
                if (bh > 0f) drawRoundRect(
                    if (i == enIyi) Turuncu else Turuncu.copy(alpha = .45f),
                    Offset(x + ic, size.height - bh), Size(gen - 2 * ic, bh), CornerRadius(8f, 8f)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth()) { etiketler.forEach { Text2(it, Soluk, 10.sp, Modifier.weight(1f), TextAlign.Center) } }
    }
}

data class Dilim(val ad: String, val deger: Double, val renk: Color, val alt: String? = null)

/** Halka grafik + açıklama listesi (pay yüzdeleriyle). */
@Composable
fun HalkaGrafik(dilimler: List<Dilim>, ortaBaslik: String, ortaDeger: String) {
    val toplam = dilimler.sumOf { it.deger }.let { if (it <= 0) 1.0 else it }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(Modifier.size(136.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(136.dp)) {
                val kalinlik = 30f
                var bas = -90f
                dilimler.forEach { d ->
                    val aci = (d.deger / toplam * 360f).toFloat()
                    drawArc(d.renk, bas, (aci - 1.5f).coerceAtLeast(0.5f), useCenter = false,
                        topLeft = Offset(kalinlik / 2, kalinlik / 2), size = Size(size.width - kalinlik, size.height - kalinlik), style = Stroke(kalinlik))
                    bas += aci
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text2(ortaBaslik, Soluk, 10.sp)
                Text2(ortaDeger, Murekkep, 14.sp, kalin = true)
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            dilimler.take(7).forEach { d ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).background(d.renk, CircleShape))
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text2(d.ad, Murekkep, 13.sp, kalin = true)
                        Text2("%" + String.format(com.teksosdoner.app.TR, "%.0f", d.deger / toplam * 100) + (d.alt?.let { " · $it" } ?: ""), Soluk, 11.sp)
                    }
                }
            }
        }
    }
}

/** Kâr/zarar şelalesi: ciro'dan başlayıp her kesintiyi çubukla gösterir. */
@Composable
fun KarSelalesi(satirlar: List<Triple<String, Double, Boolean>>, taban: Double) {
    val t = if (taban <= 0) 1.0 else taban
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        satirlar.forEach { (ad, deger, toplamSatiri) ->
            val eksi = deger < 0
            val renk = when {
                toplamSatiri && deger >= 0 -> Yesil
                toplamSatiri -> Kirmizi
                eksi -> Kirmizi.copy(alpha = .75f)
                else -> Turuncu
            }
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text2(ad, if (toplamSatiri) Murekkep else Soluk, 13.sp, Modifier.weight(1f), kalin = toplamSatiri)
                    Text2((if (eksi) "−" else "") + tl(kotuMutlak(deger)), if (toplamSatiri) renk else Murekkep, 14.sp, kalin = true)
                }
                Spacer(Modifier.height(4.dp))
                Box(Modifier.fillMaxWidth().height(if (toplamSatiri) 10.dp else 7.dp).background(Color(0xFFF1EDE7), RoundedCornerShape(5.dp))) {
                    Box(Modifier.fillMaxWidth((kotuMutlak(deger) / t).toFloat().coerceIn(0.01f, 1f)).height(if (toplamSatiri) 10.dp else 7.dp).background(renk, RoundedCornerShape(5.dp)))
                }
            }
        }
    }
}

private fun kotuMutlak(d: Double) = if (d < 0) -d else d

/** Sıralı liste satırı: sıra rozeti, ad, alt bilgi, değer ve pay çubuğu. */
@Composable
fun SiraSatir(sira: Int, ad: String, alt: String?, deger: String, pay: Float, sagAlt: String? = null, rozetRenk: Color = Turuncu) {
    Column(Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(26.dp).background(if (sira <= 3) rozetRenk else Color(0xFFF1EDE7), CircleShape), contentAlignment = Alignment.Center) {
                Text2(sira.toString(), if (sira <= 3) Color.White else Soluk, 12.sp, kalin = true)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text2(ad, Murekkep, 14.sp, kalin = true)
                if (!alt.isNullOrEmpty()) Text2(alt, Soluk, 12.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text2(deger, Murekkep, 14.sp, kalin = true)
                if (!sagAlt.isNullOrEmpty()) Text2(sagAlt, Soluk, 11.sp)
            }
        }
        Spacer(Modifier.height(6.dp))
        Box(Modifier.padding(start = 36.dp).fillMaxWidth().height(4.dp).background(Color(0xFFF1EDE7), RoundedCornerShape(2.dp))) {
            Box(Modifier.fillMaxWidth(pay.coerceIn(0.01f, 1f)).height(4.dp).background(rozetRenk.copy(alpha = .7f), RoundedCornerShape(2.dp)))
        }
    }
}

@Composable
fun Text2(
    metin: String, renk: Color, boyut: androidx.compose.ui.unit.TextUnit, modifier: Modifier = Modifier,
    hiza: TextAlign? = null, kalin: Boolean = false, satir: Int = 1,
) {
    androidx.compose.material3.Text(
        metin, modifier = modifier, color = renk, fontSize = boyut, textAlign = hiza, maxLines = satir, overflow = TextOverflow.Ellipsis,
        fontWeight = if (kalin) FontWeight.Bold else FontWeight.Normal
    )
}

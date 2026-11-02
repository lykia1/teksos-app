@file:OptIn(ExperimentalMaterial3Api::class)

package com.teksosdoner.app.ui

import com.teksosdoner.app.*

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teksosdoner.app.tl
import com.teksosdoner.app.yuzde
import kotlin.math.abs

/** Koyu üst çubuklu ekran iskeleti. */
@Composable
fun Ekran(
    baslik: String,
    altBaslik: String? = null,
    geri: (() -> Unit)? = null,
    eylemler: @Composable RowScope.() -> Unit = {},
    icerik: @Composable () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(baslik, fontWeight = FontWeight.Bold, fontSize = 19.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (!altBaslik.isNullOrEmpty()) Text(altBaslik, fontSize = 12.sp, color = Color(0xFFD6D3D1), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                },
                navigationIcon = {
                    if (geri != null) IconButton(onClick = geri) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri") }
                },
                actions = eylemler,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Koyu, titleContentColor = Color.White,
                    navigationIconContentColor = Color.White, actionIconContentColor = Color.White
                )
            )
        },
        containerColor = Zemin,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { p ->
        Box(Modifier.padding(p).fillMaxSize()) { icerik() }
    }
}

@Composable
fun Kart(
    modifier: Modifier = Modifier,
    koyu: Boolean = false,
    onClick: (() -> Unit)? = null,
    icerik: @Composable ColumnScope.() -> Unit,
) {
    val renk = CardDefaults.cardColors(containerColor = if (koyu) Koyu else Color.White, contentColor = if (koyu) Color.White else Murekkep)
    val kenar = if (koyu) null else BorderStroke(1.dp, Cizgi)
    val sekil = RoundedCornerShape(18.dp)
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = sekil, colors = renk, border = kenar) {
            Column(Modifier.padding(16.dp), content = icerik)
        }
    } else {
        Card(modifier = modifier.fillMaxWidth(), shape = sekil, colors = renk, border = kenar) {
            Column(Modifier.padding(16.dp), content = icerik)
        }
    }
}

@Composable
fun Yukleniyor() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Turuncu) }
}

@Composable
fun HataKutusu(mesaj: String, tekrar: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("⚠️", fontSize = 44.sp)
            Spacer(Modifier.height(8.dp))
            Text(mesaj, textAlign = TextAlign.Center, color = Murekkep, fontSize = 16.sp)
            Spacer(Modifier.height(16.dp))
            Button(onClick = tekrar, colors = ButtonDefaults.buttonColors(containerColor = Turuncu)) { Text("Tekrar dene") }
        }
    }
}

@Composable
fun BosKutu(metin: String) {
    Box(
        Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(14.dp)).padding(vertical = 22.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) { Text(metin, color = Soluk, textAlign = TextAlign.Center) }
}

@Composable
fun Rozet(metin: String, renk: Color, zemin: Color, modifier: Modifier = Modifier) {
    Text(
        metin, color = renk, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1,
        modifier = modifier.background(zemin, RoundedCornerShape(999.dp)).padding(horizontal = 9.dp, vertical = 3.dp)
    )
}

/** Geçen döneme göre yüzde değişim rozeti (önceki 0 ise görünmez). */
@Composable
fun DegisimRozet(simdi: Double, once: Double) {
    val y = yuzde(simdi, once) ?: return
    val artis = y >= 0
    Rozet(
        (if (artis) "▲ " else "▼ ") + "%" + String.format(com.teksosdoner.app.TR, "%.0f", abs(y)),
        if (artis) Yesil else Kirmizi, if (artis) YesilAcik else KirmiziAcik
    )
}

@Composable
fun BilgiSatir(sol: String, sag: String, renk: Color = Murekkep, kalin: Boolean = false, alt: String? = null) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(sol, color = Soluk, fontSize = 14.sp)
            if (alt != null) Text(alt, color = Soluk, fontSize = 11.sp)
        }
        Spacer(Modifier.padding(4.dp))
        Text(sag, color = renk, fontSize = 15.sp, fontWeight = if (kalin) FontWeight.ExtraBold else FontWeight.SemiBold, textAlign = TextAlign.End)
    }
}

@Composable
fun Ayrac() = HorizontalDivider(Modifier.padding(vertical = 8.dp), color = Cizgi)

@Composable
fun BolumBaslik(metin: String, modifier: Modifier = Modifier) {
    Text(
        metin.uppercase(com.teksosdoner.app.TR), color = Soluk, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp,
        modifier = modifier.padding(top = 6.dp, bottom = 2.dp)
    )
}

/** Basit çubuk grafik. karsi: arkada gri karşılaştırma çubukları (ör. geçen hafta). */
@Composable
fun Cubuklar(degerler: List<Double>, etiketler: List<String>, karsi: List<Double>? = null, yukseklik: Dp = 110.dp, vurgu: Int? = null, ustYazi: List<String>? = null) {
    val enBuyuk = ((degerler + (karsi ?: emptyList())).maxOrNull() ?: 0.0).let { if (it <= 0) 1.0 else it }
    Column(Modifier.fillMaxWidth()) {
        if (ustYazi != null) {
            Row(Modifier.fillMaxWidth()) {
                ustYazi.forEach { Text(it, Modifier.weight(1f), fontSize = 9.sp, color = Soluk, textAlign = TextAlign.Center, maxLines = 1) }
            }
        }
        Canvas(Modifier.fillMaxWidth().height(yukseklik)) {
            val n = degerler.size
            if (n == 0) return@Canvas
            val aralik = size.width / n
            val gen = aralik * 0.64f
            degerler.forEachIndexed { i, v ->
                val x = i * aralik + (aralik - gen) / 2f
                karsi?.getOrNull(i)?.let { k ->
                    val h = (k / enBuyuk * size.height).toFloat()
                    if (h > 0f) drawRoundRect(Cizgi, Offset(x, size.height - h), Size(gen, h), CornerRadius(5f, 5f))
                }
                val h = (v / enBuyuk * size.height).toFloat().coerceAtLeast(if (v > 0) 3f else 0f)
                val ic = if (karsi != null) gen * 0.22f else 0f
                val renk = if ((vurgu ?: (n - 1)) == i) Turuncu else Turuncu.copy(alpha = 0.5f)
                if (h > 0f) drawRoundRect(renk, Offset(x + ic, size.height - h), Size(gen - 2 * ic, h), CornerRadius(7f, 7f))
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            etiketler.forEach { Text(it, Modifier.weight(1f), fontSize = 10.sp, color = Soluk, textAlign = TextAlign.Center, maxLines = 1) }
        }
    }
}

/** Ad – tutar listesi, tutara göre ince çubukla. */
@Composable
fun DagilimListe(satirlar: List<Triple<String, Double, String?>>) {
    val toplam = satirlar.sumOf { it.second }.let { if (it <= 0) 1.0 else it }
    satirlar.forEach { (ad, t, alt) ->
        Column(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(ad + (if (alt != null) "  $alt" else ""), Modifier.weight(1f), fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(tl(t), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
            Spacer(Modifier.height(4.dp))
            Box(Modifier.fillMaxWidth().height(5.dp).background(Color(0xFFF1EDE7), RoundedCornerShape(3.dp))) {
                Box(Modifier.fillMaxWidth((t / toplam).toFloat().coerceIn(0.02f, 1f)).height(5.dp).background(Turuncu.copy(alpha = 0.75f), RoundedCornerShape(3.dp)))
            }
        }
    }
}

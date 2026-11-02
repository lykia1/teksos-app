package com.teksosdoner.app.ui

import com.teksosdoner.app.*

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Renkli yuvarlak zeminde ikon. */
@Composable
fun IkonRozet(ikon: ImageVector, renk: Color, boyut: Int = 38) {
    Box(Modifier.size(boyut.dp).background(renk.copy(alpha = .12f), RoundedCornerShape((boyut * 0.32f).dp)), contentAlignment = Alignment.Center) {
        Icon(ikon, contentDescription = null, tint = renk, modifier = Modifier.size((boyut * 0.55f).dp))
    }
}

/** Bilgi kutucuğu (KPI): başlık, büyük değer, alt satır ve değişim rozeti. */
@Composable
fun KpiKutu(
    baslik: String, deger: String, ikon: ImageVector, renk: Color, modifier: Modifier = Modifier,
    alt: String? = null, simdi: Double? = null, once: Double? = null, koyu: Boolean = false,
) {
    Column(
        modifier
            .background(if (koyu) Koyu else Color.White, RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IkonRozet(ikon, if (koyu) Color(0xFFFDBA74) else renk, 30)
            Spacer(Modifier.width(8.dp))
            Text(baslik, color = if (koyu) Color(0xFFD6D3D1) else Soluk, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(10.dp))
        Text(deger, color = if (koyu) Color.White else Murekkep, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
            if (simdi != null && once != null) { DegisimRozet(simdi, once); Spacer(Modifier.width(6.dp)) }
            if (alt != null) Text(alt, color = if (koyu) Color(0xFFA8A29E) else Soluk, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** İki sütunlu kutucuk ızgarası. */
@Composable
fun KpiIzgara(icerik: List<@Composable (Modifier) -> Unit>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        icerik.chunked(2).forEach { satir ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                satir.forEach { it(Modifier.weight(1f)) }
                if (satir.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** Kart başlığı: ikon + başlık + sağda isteğe bağlı açıklama. */
@Composable
fun KartBaslik(ikon: ImageVector, baslik: String, sag: String? = null, renk: Color = Turuncu) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        IkonRozet(ikon, renk, 32)
        Spacer(Modifier.width(10.dp))
        Text(baslik, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Murekkep, modifier = Modifier.weight(1f))
        if (sag != null) Text(sag, color = Soluk, fontSize = 12.sp)
    }
}

/** Bilgi / uyarı şeridi. */
@Composable
fun Serit(ikon: ImageVector, metin: String, renk: Color, zemin: Color) {
    Row(Modifier.fillMaxWidth().background(zemin, RoundedCornerShape(14.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(ikon, null, tint = renk, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(metin, color = renk, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

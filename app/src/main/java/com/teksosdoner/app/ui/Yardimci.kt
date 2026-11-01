@file:OptIn(ExperimentalMaterial3Api::class)

package com.teksosdoner.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teksosdoner.app.TR
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Ortak çip (filtre / sekme). */
@Composable
fun Cip2(secili: Boolean, metin: String, sayi: Int = 0, tikla: () -> Unit) {
    FilterChip(
        selected = secili, onClick = tikla, shape = RoundedCornerShape(999.dp),
        label = {
            Text(metin, fontWeight = FontWeight.SemiBold)
            if (sayi > 0) {
                Spacer(Modifier.width(6.dp))
                Text(sayi.toString(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.background(Kirmizi, RoundedCornerShape(999.dp)).padding(horizontal = 6.dp, vertical = 1.dp))
            }
        },
        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Koyu, selectedLabelColor = Color.White, containerColor = Color.White, labelColor = Murekkep)
    )
}

/** "‹ Kasım 2026 ›" ay seçici. ay: "2026-11" */
@Composable
fun AySecici(ay: String, degis: (String) -> Unit) {
    val ad = runCatching {
        SimpleDateFormat("MMMM yyyy", TR).format(SimpleDateFormat("yyyy-MM", Locale.US).parse(ay)!!).replaceFirstChar { it.titlecase(TR) }
    }.getOrDefault(ay)
    fun kay(n: Int) {
        val c = Calendar.getInstance()
        c.time = SimpleDateFormat("yyyy-MM", Locale.US).parse(ay)!!
        c.add(Calendar.MONTH, n)
        degis(SimpleDateFormat("yyyy-MM", Locale.US).format(c.time))
    }
    Row(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(14.dp)).padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { kay(-1) }) { Icon(Icons.Filled.ChevronLeft, "Önceki ay", tint = Turuncu) }
        Text(ad, Modifier.weight(1f), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        IconButton(onClick = { kay(1) }) { Icon(Icons.Filled.ChevronRight, "Sonraki ay", tint = Turuncu) }
    }
}

fun buAy(): String = SimpleDateFormat("yyyy-MM", Locale.US).format(Calendar.getInstance().time)
fun bugunTarih(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time)

/** Onay penceresi (silme vb.). */
@Composable
fun OnayPenceresi(baslik: String, metin: String, evet: String, tehlikeli: Boolean = true, kapat: () -> Unit, onayla: () -> Unit) {
    AlertDialog(
        onDismissRequest = kapat,
        title = { Text(baslik, fontWeight = FontWeight.Bold) },
        text = { Text(metin) },
        confirmButton = { TextButton(onClick = { kapat(); onayla() }) { Text(evet, color = if (tehlikeli) Kirmizi else Turuncu, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = kapat) { Text("Vazgeç", color = Soluk) } }
    )
}

/** Açılır seçim kutusu. */
@Composable
fun SecimKutusu(etiket: String, secenekler: List<Pair<String, String>>, secili: String, modifier: Modifier = Modifier, sec: (String) -> Unit) {
    var acik by remember { mutableStateOf(false) }
    Column(modifier) {
        Text(etiket, color = Soluk, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 4.dp))
        Box {
            OutlinedButton(onClick = { acik = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Text(secenekler.firstOrNull { it.first == secili }?.second ?: "Seçin", Modifier.weight(1f), color = Murekkep, maxLines = 1)
                Icon(Icons.Filled.ArrowDropDown, null, tint = Soluk)
            }
            DropdownMenu(expanded = acik, onDismissRequest = { acik = false }) {
                secenekler.forEach { (k, a) -> DropdownMenuItem(text = { Text(a) }, onClick = { sec(k); acik = false }) }
            }
        }
    }
}

@Composable
fun alanRenkleri() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Turuncu, focusedLabelColor = Turuncu, cursorColor = Turuncu,
    unfocusedContainerColor = Color.White, focusedContainerColor = Color.White
)

/** Kısa durum mesajı şeridi (işlem sonucu). */
@Composable
fun MesajSeridi(mesaj: String?, hata: Boolean, kapat: () -> Unit) {
    if (mesaj == null) return
    Row(
        Modifier.fillMaxWidth().background(if (hata) KirmiziAcik else Koyu, RoundedCornerShape(12.dp)).padding(start = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(mesaj, color = if (hata) Kirmizi else Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f).padding(vertical = 10.dp))
        TextButton(onClick = kapat) { Text("Tamam", color = if (hata) Kirmizi else Color(0xFFFDBA74)) }
    }
}

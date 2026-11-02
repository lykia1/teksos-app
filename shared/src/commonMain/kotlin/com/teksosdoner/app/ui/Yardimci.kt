@file:OptIn(ExperimentalMaterial3Api::class)

package com.teksosdoner.app.ui

import com.teksosdoner.app.*
import androidx.compose.material3.*

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
    val ad = ay.drop(5).take(2).toIntOrNull()?.let { ayAdi(it) + " " + ay.take(4) } ?: ay
    fun kay(n: Int) = degis(ayEkle(ay, n))
    Row(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(14.dp)).padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { kay(-1) }) { Icon(Icons.Filled.ChevronLeft, "Önceki ay", tint = Turuncu) }
        Text(ad, Modifier.weight(1f), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        IconButton(onClick = { kay(1) }) { Icon(Icons.Filled.ChevronRight, "Sonraki ay", tint = Turuncu) }
    }
}

fun buAy(): String = buAyMetin()
fun bugunTarih(): String = bugunMetin()

/* ---------- Tarih / saat seçici (iki telefonda aynı görünen pencere) ---------- */

private class SeciciIstek(val tarih: Boolean, val baslik: String?, val baslangic: String, val sonuc: (String) -> Unit)

private object Secici {
    var istek by mutableStateOf<SeciciIstek?>(null)
}

/** Tarih seçtirir; sonuç "yyyy-MM-dd". */
fun tarihSec(baslik: String? = null, baslangic: String = bugunTarih(), sonuc: (String) -> Unit) {
    Secici.istek = SeciciIstek(true, baslik, baslangic, sonuc)
}

/** Saat seçtirir; sonuç "HH:mm". */
fun saatSec(baslangic: String = "11:00", sonuc: (String) -> Unit) {
    Secici.istek = SeciciIstek(false, null, baslangic, sonuc)
}

/** Uygulamanın en üstüne bir kez konur; seçici pencereleri burada açılır. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeciciKatmani() {
    val i = Secici.istek ?: return
    val kapat = { Secici.istek = null }
    if (i.tarih) {
        val ilk = tarihParcala(i.baslangic) ?: tarihParcala(bugunTarih())!!
        val ms = remember(i) { gunSayisi(ilk.first, ilk.second, ilk.third) * 86_400_000L }
        val durum = rememberDatePickerState(initialSelectedDateMillis = ms)
        DatePickerDialog(
            onDismissRequest = kapat,
            confirmButton = {
                TextButton(onClick = {
                    val secilen = durum.selectedDateMillis
                    kapat()
                    secilen?.let { i.sonuc(tarihGunden((it / 86_400_000L).toInt())) }
                }) { Text("Tamam", color = Turuncu, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = kapat) { Text("Vazgeç", color = Soluk) } }
        ) {
            DatePicker(durum, title = i.baslik?.let { b -> { Text(b, Modifier.padding(start = 24.dp, top = 16.dp), fontWeight = FontWeight.Bold) } },
                colors = DatePickerDefaults.colors(selectedDayContainerColor = Turuncu, todayDateBorderColor = Turuncu, todayContentColor = Turuncu))
        }
    } else {
        val p = i.baslangic.split(":")
        val durum = rememberTimePickerState(p.getOrNull(0)?.toIntOrNull() ?: 11, p.getOrNull(1)?.toIntOrNull() ?: 0, is24Hour = true)
        AlertDialog(
            onDismissRequest = kapat,
            confirmButton = {
                TextButton(onClick = {
                    kapat()
                    i.sonuc(durum.hour.toString().padStart(2, '0') + ":" + durum.minute.toString().padStart(2, '0'))
                }) { Text("Tamam", color = Turuncu, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = kapat) { Text("Vazgeç", color = Soluk) } },
            text = { TimePicker(durum, colors = TimePickerDefaults.colors(selectorColor = Turuncu)) }
        )
    }
}

/** 1970-01-01'den bu yana gün sayısı (UTC) */
private fun gunSayisi(y: Int, a: Int, g: Int): Long {
    var n = 0L
    for (yy in 1970 until y) n += if ((yy % 4 == 0 && yy % 100 != 0) || yy % 400 == 0) 366 else 365
    for (aa in 1 until a) n += ayGunSayisi(y, aa)
    return n + g - 1
}

private fun tarihGunden(gun: Int): String = tarihEkle("1970-01-01", gun)

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

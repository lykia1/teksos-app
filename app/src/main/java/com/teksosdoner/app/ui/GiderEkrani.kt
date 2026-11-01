@file:OptIn(ExperimentalMaterial3Api::class)

package com.teksosdoner.app.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teksosdoner.app.Api
import com.teksosdoner.app.ApiHata
import com.teksosdoner.app.gunAdi
import com.teksosdoner.app.kisaTarih
import com.teksosdoner.app.nesneler
import com.teksosdoner.app.tl
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Calendar

@Composable
fun GiderEkrani(geri: (() -> Unit)?) {
    val kapsam = rememberCoroutineScope()
    var ay by rememberSaveable { mutableStateOf(buAy()) }
    var veri by remember { mutableStateOf<JSONObject?>(null) }
    var hata by remember { mutableStateOf<String?>(null) }
    var yukleniyor by remember { mutableStateOf(false) }
    var mesaj by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    var duzenle by remember { mutableStateOf<JSONObject?>(null) }
    var silinecek by remember { mutableStateOf<JSONObject?>(null) }

    suspend fun yukle() {
        yukleniyor = true
        try { veri = Api.get("gider", mapOf("ay" to ay)); hata = null } catch (e: ApiHata) { hata = e.message } finally { yukleniyor = false }
    }
    fun islem(govde: JSONObject, sonra: () -> Unit = {}) {
        kapsam.launch {
            try { mesaj = Api.post("gider_islem", govde).optString("mesaj") to false; sonra() } catch (e: ApiHata) { mesaj = (e.message ?: "Hata") to true }
            yukle()
        }
    }
    LaunchedEffect(ay) { yukle() }

    Ekran("Gelir & Gider", altBaslik = "Kasa dışı harcamalar", geri = geri, eylemler = {
        IconButton(onClick = { kapsam.launch { yukle() } }) { Icon(Icons.Filled.Refresh, "Yenile") }
    }) {
        Box(Modifier.fillMaxSize()) {
            val v = veri
            when {
                v == null && hata != null -> HataKutusu(hata!!) { kapsam.launch { yukle() } }
                v == null -> Yukleniyor()
                else -> PullToRefreshBox(isRefreshing = yukleniyor, onRefresh = { kapsam.launch { yukle() } }, modifier = Modifier.fillMaxSize()) {
                    val liste = v.optJSONArray("liste").nesneler()
                    val ozet = v.optJSONArray("ozet").nesneler()
                    LazyColumn(contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 90.dp), verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                        item { AySecici(ay) { ay = it } }
                        mesaj?.let { (m, h) -> item { MesajSeridi(m, h) { mesaj = null } } }
                        item {
                            KpiIzgara(listOf<@Composable (Modifier) -> Unit>(
                                { m -> KpiKutu("Toplam gider", tl(v.optDouble("toplam")), Icons.Filled.Payments, Kirmizi, m, koyu = true, simdi = v.optDouble("toplam"), once = v.optDouble("onceki"), alt = "önceki aya göre") },
                                { m -> KpiKutu("Nakit (kasadan)", tl(v.optDouble("nakit")), Icons.Filled.Money, Sari, m, alt = "gün sonu kasasını etkiler") },
                            ))
                        }
                        if (ozet.isNotEmpty()) item {
                            Kart {
                                KartBaslik(Icons.Filled.Category, "Kategorilere göre", renk = Kirmizi)
                                val enBuyuk = ozet.maxOfOrNull { it.optDouble("t") } ?: 1.0
                                ozet.forEachIndexed { i, o -> SiraSatir(i + 1, o.optString("ad"), "${o.optInt("n")} kayıt", tl(o.optDouble("t")), (o.optDouble("t") / enBuyuk).toFloat(), rozetRenk = Kirmizi) }
                            }
                        }
                        item { BolumBaslik("Kayıtlar (${liste.size})", Modifier.padding(start = 4.dp)) }
                        if (liste.isEmpty()) item { BosKutu("Bu ay gider kaydı yok.") }
                        items(liste, key = { it.optInt("id") }) { g ->
                            val kilitli = g.optBoolean("depo") || g.optBoolean("avans")
                            Kart(onClick = if (kilitli) null else ({ duzenle = g })) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IkonRozet(
                                        when { g.optBoolean("depo") -> Icons.Filled.Inventory2; g.optBoolean("avans") -> Icons.Filled.Wallet; g.optString("yon") == "nakit" -> Icons.Filled.Money; else -> Icons.Filled.CreditCard },
                                        if (g.optString("yon") == "nakit") Sari else Mavi, 38
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(g.optString("kat"), fontWeight = FontWeight.Bold)
                                        Text(listOf(kisaTarih(g.optString("tarih")) + " " + gunAdi(g.optString("tarih")), g.optString("aciklama").ifEmpty { null }, g.optString("ekleyen").ifEmpty { null })
                                            .filterNotNull().joinToString(" · "), color = Soluk, fontSize = 12.sp, maxLines = 2)
                                        if (g.optBoolean("depo")) Text("Depo alımına bağlı · Depo & Stok'tan düzenlenir", color = Mavi, fontSize = 11.sp)
                                        if (g.optBoolean("avans")) Text("Personel avansı · Maaş & Avans'tan düzenlenir", color = Mavi, fontSize = 11.sp)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(tl(g.optDouble("tutar")), fontWeight = FontWeight.ExtraBold)
                                        Text(if (g.optString("yon") == "nakit") "Nakit" else "Kart / banka", color = Soluk, fontSize = 11.sp)
                                    }
                                }
                                if (!kilitli) TextButton(onClick = { silinecek = g }, modifier = Modifier.align(Alignment.End)) { Text("Sil", color = Kirmizi) }
                            }
                        }
                    }
                }
            }
            ExtendedFloatingActionButton(
                onClick = { duzenle = JSONObject().put("id", 0).put("tarih", bugunTarih()).put("yon", "nakit") },
                containerColor = Turuncu, contentColor = androidx.compose.ui.graphics.Color.White,
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                icon = { Icon(Icons.Filled.Add, null) }, text = { Text("Gider ekle", fontWeight = FontWeight.Bold) }
            )
        }
    }

    duzenle?.let { g ->
        GiderPenceresi(g, veri?.optJSONArray("kategoriler").nesneler().filter { it.optInt("id") != (veri?.optInt("avans_kat") ?: -1) }, kapat = { duzenle = null }) { govde ->
            islem(govde) { duzenle = null }
        }
    }
    silinecek?.let { g ->
        OnayPenceresi("Gider silinsin mi?", "${g.optString("kat")} · ${tl(g.optDouble("tutar"))} kaydı silinecek.", "Sil", kapat = { silinecek = null }) {
            islem(JSONObject().put("act", "sil").put("id", g.optInt("id")))
        }
    }
}

@Composable
private fun GiderPenceresi(g: JSONObject, kategoriler: List<JSONObject>, kapat: () -> Unit, kaydet: (JSONObject) -> Unit) {
    val ctx = LocalContext.current
    var kat by remember { mutableStateOf(g.optInt("kat_id").let { if (it > 0) it.toString() else "" }) }
    var tutar by remember { mutableStateOf(if (g.optDouble("tutar", 0.0) > 0) g.optDouble("tutar").toString().replace('.', ',').removeSuffix(",0") else "") }
    var yon by remember { mutableStateOf(g.optString("yon", "nakit")) }
    var tarih by remember { mutableStateOf(g.optString("tarih").ifEmpty { bugunTarih() }) }
    var aciklama by remember { mutableStateOf(g.optString("aciklama")) }
    AlertDialog(
        onDismissRequest = kapat,
        title = { Text(if (g.optInt("id") > 0) "Gideri düzenle" else "Yeni gider", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SecimKutusu("Kategori", kategoriler.map { it.optInt("id").toString() to it.optString("ad") }, kat) { kat = it }
                OutlinedTextField(tutar, { tutar = it.filter { c -> c.isDigit() || c == ',' || c == '.' } }, label = { Text("Tutar (₺)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), colors = alanRenkleri(), modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Cip2(yon == "nakit", "Nakit (kasadan)") { yon = "nakit" }
                    Cip2(yon == "kredi_karti", "Kart / banka") { yon = "kredi_karti" }
                }
                OutlinedButton(onClick = {
                    val c = Calendar.getInstance()
                    DatePickerDialog(ctx, { _, y, m, d -> tarih = String.format(java.util.Locale.US, "%04d-%02d-%02d", y, m + 1, d) },
                        c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Filled.CalendarMonth, null, tint = Turuncu); Spacer(Modifier.width(8.dp))
                    Text(kisaTarih(tarih) + "." + tarih.take(4) + " " + gunAdi(tarih), color = Murekkep)
                }
                OutlinedTextField(aciklama, { aciklama = it.take(250) }, label = { Text("Açıklama (isteğe bağlı)") }, colors = alanRenkleri(), modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    kaydet(JSONObject().put("id", g.optInt("id")).put("kategori_id", kat.toIntOrNull() ?: 0).put("tutar", tutar).put("odeme_yontemi", yon).put("tarih", tarih).put("aciklama", aciklama))
                },
                enabled = kat.isNotEmpty() && tutar.isNotEmpty(), colors = ButtonDefaults.buttonColors(containerColor = Turuncu)
            ) { Text("Kaydet", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = kapat) { Text("Vazgeç", color = Soluk) } }
    )
}

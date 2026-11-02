@file:OptIn(ExperimentalMaterial3Api::class)

package com.teksosdoner.app.ui

import com.teksosdoner.app.*

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RemoveShoppingCart
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teksosdoner.app.Api
import com.teksosdoner.app.ApiHata
import com.teksosdoner.app.TR
import com.teksosdoner.app.kisaTarih
import com.teksosdoner.app.nesneler
import com.teksosdoner.app.tl
import kotlinx.coroutines.launch
import com.teksosdoner.app.json.JSONArray
import com.teksosdoner.app.json.JSONObject

private fun miktar(d: Double): String = if (d % 1.0 == 0.0) d.toLong().toString() else String.format(TR, "%.3f", d).trimEnd('0').trimEnd(',')

@Composable
fun StokEkrani(geri: (() -> Unit)?) {
    val kapsam = rememberCoroutineScope()
    var veri by remember { mutableStateOf<JSONObject?>(null) }
    var hata by remember { mutableStateOf<String?>(null) }
    var yukleniyor by remember { mutableStateOf(false) }
    var filtre by rememberSaveable { mutableStateOf("hepsi") }
    var ara by rememberSaveable { mutableStateOf("") }
    var mesaj by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    var sayim by remember { mutableStateOf<JSONObject?>(null) }

    suspend fun yukle() {
        yukleniyor = true
        try { veri = Api.get("stok"); hata = null } catch (e: ApiHata) { hata = e.message } finally { yukleniyor = false }
    }
    LaunchedEffect(Unit) { yukle() }

    Ekran("Depo & Stok", altBaslik = "Satış yapıldıkça reçeteye göre düşer", geri = geri, eylemler = {
        IconButton(onClick = { kapsam.launch { yukle() } }) { Icon(Icons.Filled.Refresh, "Yenile") }
    }) {
        val v = veri
        when {
            v == null && hata != null -> HataKutusu(hata!!) { kapsam.launch { yukle() } }
            v == null -> Yukleniyor()
            else -> PullToRefreshBox(isRefreshing = yukleniyor, onRefresh = { kapsam.launch { yukle() } }, modifier = Modifier.fillMaxSize()) {
                val o = v.optJSONObject("ozet") ?: JSONObject()
                val tum = v.optJSONArray("urunler").nesneler()
                val q = ara.trim().lowercase(TR)
                val liste = tum.filter { u ->
                    val s = u.optDouble("stok"); val min = u.optDouble("min")
                    (q.isEmpty() || u.optString("ad").lowercase(TR).contains(q) || u.optString("kat").lowercase(TR).contains(q)) && when (filtre) {
                        "kritik" -> min > 0 && s <= min
                        "biten" -> s == 0.0
                        "eksi" -> s < 0
                        else -> true
                    }
                }
                LazyColumn(contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                    mesaj?.let { (m, h) -> item { MesajSeridi(m, h) { mesaj = null } } }
                    item {
                        KpiIzgara(listOf<@Composable (Modifier) -> Unit>(
                            { m -> KpiKutu("Depodaki mal", tl(o.optDouble("deger")), Icons.Filled.Warehouse, Turuncu, m, koyu = true, alt = "${o.optInt("adet")} stok kartı") },
                            { m -> KpiKutu("Tedarikçi borcu", tl(o.optDouble("borc")), Icons.Filled.AccountBalance, Kirmizi, m, alt = if (o.optDouble("alacak") > 0) "alacak " + tl(o.optDouble("alacak")) else "cari hesaplar") },
                            { m -> KpiKutu("Kritik seviyede", o.optInt("kritik").toString(), Icons.Filled.Warning, Sari, m, alt = "sipariş verilmeli") },
                            { m -> KpiKutu("Biten / eksi", "${o.optInt("biten")} / ${o.optInt("eksi")}", Icons.Filled.RemoveShoppingCart, Kirmizi, m, alt = "sayım veya alım gerekli") },
                        ))
                    }
                    item {
                        OutlinedTextField(ara, { ara = it }, placeholder = { Text("Ürün veya grup ara") }, singleLine = true, leadingIcon = { Icon(Icons.Filled.Search, null) },
                            trailingIcon = { if (ara.isNotEmpty()) IconButton(onClick = { ara = "" }) { Icon(Icons.Filled.Clear, "Temizle") } },
                            shape = RoundedCornerShape(14.dp), colors = alanRenkleri(), modifier = Modifier.fillMaxWidth())
                    }
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item { Cip2(filtre == "hepsi", "Tümü (${tum.size})") { filtre = "hepsi" } }
                            item { Cip2(filtre == "kritik", "Kritik", o.optInt("kritik")) { filtre = "kritik" } }
                            item { Cip2(filtre == "biten", "Biten (${o.optInt("biten")})") { filtre = "biten" } }
                            item { Cip2(filtre == "eksi", "Eksiye düşen (${o.optInt("eksi")})") { filtre = "eksi" } }
                        }
                    }
                    item { Text("Saydığınız ürüne dokunup gerçek miktarı girin; sistem stoğu düzeltilir.", color = Soluk, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp)) }
                    if (liste.isEmpty()) item { BosKutu("Bu filtrede ürün yok.") }
                    items(liste, key = { it.optInt("id") }) { u ->
                        val s = u.optDouble("stok"); val min = u.optDouble("min")
                        val renk = when { s < 0 -> Kirmizi; min > 0 && s <= min -> Sari; s == 0.0 -> Soluk; else -> Yesil }
                        Kart(onClick = { sayim = u }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IkonRozet(Icons.Filled.Inventory2, renk, 38)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(u.optString("ad"), fontWeight = FontWeight.Bold)
                                    Text(listOf(u.optString("kat").ifEmpty { null }, if (min > 0) "min ${miktar(min)}" else null,
                                        u.optString("son_sayim").ifEmpty { null }?.let { "sayım " + kisaTarih(it) }).filterNotNull().joinToString(" · "), color = Soluk, fontSize = 12.sp)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(miktar(s) + " " + u.optString("birim"), fontWeight = FontWeight.ExtraBold, color = renk)
                                    if (s > 0 && u.optDouble("fiyat") > 0) Text(tl(s * u.optDouble("fiyat")), color = Soluk, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    sayim?.let { u ->
        var deger by remember(u) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { sayim = null },
            title = { Text(u.optString("ad"), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Sistemde: ${miktar(u.optDouble("stok"))} ${u.optString("birim")}", color = Soluk)
                    OutlinedTextField(deger, { deger = it.filter { c -> c.isDigit() || c == ',' || c == '.' } }, label = { Text("Saydığınız miktar (${u.optString("birim")})") },
                        singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), colors = alanRenkleri(), modifier = Modifier.fillMaxWidth())
                    Text("Kaydedince sistem stoğu bu miktara eşitlenir; fark tüketim analizinde fire olarak görünür.", color = Soluk, fontSize = 12.sp)
                }
            },
            confirmButton = {
                Button(onClick = {
                    val id = u.optInt("id")
                    sayim = null
                    kapsam.launch {
                        try {
                            mesaj = Api.post("stok_sayim", JSONObject().put("sayimlar", JSONArray().put(JSONObject().put("id", id).put("miktar", deger)))).optString("mesaj") to false
                        } catch (e: ApiHata) { mesaj = (e.message ?: "Hata") to true }
                        yukle()
                    }
                }, enabled = deger.isNotEmpty(), colors = ButtonDefaults.buttonColors(containerColor = Turuncu)) { Text("Sayımı kaydet", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { sayim = null }) { Text("Vazgeç", color = Soluk) } }
        )
    }
}

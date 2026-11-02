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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.TableRestaurant
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teksosdoner.app.Api
import com.teksosdoner.app.ApiHata
import com.teksosdoner.app.durumAdi
import com.teksosdoner.app.kisaTarih
import com.teksosdoner.app.metinler
import com.teksosdoner.app.nesneler
import com.teksosdoner.app.tarihSaat
import com.teksosdoner.app.tipAdi
import com.teksosdoner.app.tl
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.teksosdoner.app.json.JSONObject

@Composable
fun DurumRozet(durum: String) {
    when (durum) {
        "paid" -> Rozet("Ödendi", Yesil, YesilAcik)
        "open" -> Rozet("Açık", Sari, SariAcik)
        "cancelled" -> Rozet("İptal", Kirmizi, KirmiziAcik)
        else -> Rozet(durum, Soluk, Color(0xFFF5F5F4))
    }
}

@Composable
private fun Cip(secili: Boolean, metin: String, tikla: () -> Unit) {
    FilterChip(
        selected = secili, onClick = tikla, label = { Text(metin, fontWeight = FontWeight.SemiBold) },
        shape = RoundedCornerShape(999.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Koyu, selectedLabelColor = Color.White, containerColor = Color.White, labelColor = Murekkep
        )
    )
}

@Composable
fun SiparislerEkrani(ac: (Int) -> Unit) {
    val kapsam = rememberCoroutineScope()
    var aralik by rememberSaveable { mutableStateOf("bugun") }
    var ozelTarih by rememberSaveable { mutableStateOf("") }
    var durum by rememberSaveable { mutableStateOf("") }
    var tip by rememberSaveable { mutableStateOf("") }
    var ara by rememberSaveable { mutableStateOf("") }
    var araSon by remember { mutableStateOf("") }
    var liste by remember { mutableStateOf(listOf<JSONObject>()) }
    var ozet by remember { mutableStateOf<JSONObject?>(null) }
    var sayfa by remember { mutableIntStateOf(1) }
    var devam by remember { mutableStateOf(false) }
    var yukleniyor by remember { mutableStateOf(true) }
    var yenileniyor by remember { mutableStateOf(false) }
    var hata by remember { mutableStateOf<String?>(null) }
    var donem by remember { mutableStateOf("") }

    suspend fun yukle(yeniSayfa: Int) {
        try {
            val p = mutableMapOf("durum" to durum, "tip" to tip, "ara" to araSon, "sayfa" to yeniSayfa.toString())
            if (aralik == "ozel" && ozelTarih.isNotEmpty()) { p["bas"] = ozelTarih; p["bit"] = ozelTarih } else p["aralik"] = aralik
            val j = Api.get("siparisler", p)
            val yeni = j.optJSONArray("liste").nesneler()
            liste = if (yeniSayfa == 1) yeni else liste + yeni
            ozet = j.optJSONObject("ozet")
            devam = j.optBoolean("devam")
            sayfa = yeniSayfa
            val b = j.optString("bas"); val t = j.optString("bit")
            donem = if (b == t) kisaTarih(b) else kisaTarih(b) + " – " + kisaTarih(t)
            hata = null
        } catch (e: ApiHata) {
            hata = e.message
        }
    }
    LaunchedEffect(ara) { delay(450); araSon = ara.trim() }
    LaunchedEffect(aralik, ozelTarih, durum, tip, araSon) { yukleniyor = true; yukle(1); yukleniyor = false }

    fun gunSec() {
        tarihSec("Gün seçin") { ozelTarih = it; aralik = "ozel" }
    }

    Ekran("Siparişler", altBaslik = ozet?.let { "$donem · ${it.optInt("adet")} kayıt · ödenen ${tl(it.optDouble("ciro"))}" }, eylemler = {
        IconButton(onClick = { kapsam.launch { yenileniyor = true; yukle(1); yenileniyor = false } }) { Icon(Icons.Filled.Refresh, "Yenile") }
    }) {
        PullToRefreshBox(isRefreshing = yenileniyor, onRefresh = { kapsam.launch { yenileniyor = true; yukle(1); yenileniyor = false } }, modifier = Modifier.fillMaxSize()) {
            LazyColumn(contentPadding = PaddingValues(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                item {
                    Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LazyRow(contentPadding = PaddingValues(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item { Cip(aralik == "bugun", "Bugün") { aralik = "bugun" } }
                            item { Cip(aralik == "dun", "Dün") { aralik = "dun" } }
                            item { Cip(aralik == "hafta", "Bu hafta") { aralik = "hafta" } }
                            item { Cip(aralik == "ay", "Bu ay") { aralik = "ay" } }
                            item { Cip(aralik == "ozel", if (aralik == "ozel") kisaTarih(ozelTarih) else "Tarih seç") { gunSec() } }
                        }
                        LazyRow(contentPadding = PaddingValues(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item { Cip(durum == "", "Tümü") { durum = "" } }
                            item { Cip(durum == "paid", "Ödenen") { durum = "paid" } }
                            item { Cip(durum == "open", "Açık") { durum = "open" } }
                            item { Cip(durum == "cancelled", "İptal") { durum = "cancelled" } }
                            item { Spacer(Modifier.width(8.dp)) }
                            item { Cip(tip == "table", "Masa") { tip = if (tip == "table") "" else "table" } }
                            item { Cip(tip == "takeaway", "Al götür") { tip = if (tip == "takeaway") "" else "takeaway" } }
                            item { Cip(tip == "delivery", "Paket") { tip = if (tip == "delivery") "" else "delivery" } }
                        }
                        OutlinedTextField(
                            ara, { ara = it }, placeholder = { Text("Sipariş no, müşteri, telefon") }, singleLine = true,
                            leadingIcon = { Icon(Icons.Filled.Search, null) },
                            trailingIcon = { if (ara.isNotEmpty()) IconButton(onClick = { ara = "" }) { Icon(Icons.Filled.Clear, "Temizle") } },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Turuncu, unfocusedContainerColor = Color.White, focusedContainerColor = Color.White),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp)
                        )
                    }
                }
                when {
                    yukleniyor && liste.isEmpty() -> item { Row(Modifier.fillMaxWidth().padding(30.dp), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator(color = Turuncu) } }
                    hata != null && liste.isEmpty() -> item { Column(Modifier.padding(horizontal = 14.dp)) { BosKutu("⚠ " + hata!!) } }
                    liste.isEmpty() -> item { Column(Modifier.padding(horizontal = 14.dp)) { BosKutu("Bu filtrelerle sipariş bulunamadı.") } }
                    else -> {
                        items(liste, key = { it.optInt("id") }) { o -> Column(Modifier.padding(horizontal = 14.dp)) { SiparisSatir(o) { ac(o.optInt("id")) } } }
                        if (devam) item {
                            OutlinedButton(onClick = { kapsam.launch { yukle(sayfa + 1) } }, modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp)) { Text("Daha fazla göster", color = Turuncu) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SiparisSatir(o: JSONObject, tikla: () -> Unit) {
    val tipAd = tipAdi(o.optString("tip"))
    val alt = listOf(
        o.optString("masa").ifEmpty { null }, o.optString("musteri").ifEmpty { null }, o.optString("kanal").ifEmpty { null },
        o.optString("odeme").ifEmpty { null }
    ).filterNotNull().joinToString(" · ")
    Kart(onClick = tikla) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IkonRozet(
                when (o.optString("tip")) { "table" -> Icons.Filled.TableRestaurant; "delivery" -> Icons.Filled.DeliveryDining; else -> Icons.Filled.ShoppingBag },
                when (o.optString("tip")) { "table" -> Mavi; "delivery" -> Yesil; else -> Turuncu }, 40
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(o.optString("no"), fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("$tipAd · ${o.optString("saat")}" + (if (alt.isNotEmpty()) " · $alt" else ""), color = Soluk, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (o.optString("kurye").isNotEmpty()) Text("Kurye: " + o.optString("kurye"), color = Soluk, fontSize = 12.sp)
            }
            Spacer(Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(tl(o.optDouble("tutar")), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = if (o.optString("durum") == "cancelled") Soluk else Murekkep)
                Spacer(Modifier.height(4.dp))
                DurumRozet(o.optString("durum"))
            }
        }
    }
}

@Composable
fun SiparisDetay(id: Int, geri: () -> Unit) {
    val kapsam = rememberCoroutineScope()
    var s by remember { mutableStateOf<JSONObject?>(null) }
    var hata by remember { mutableStateOf<String?>(null) }
    suspend fun yukle() {
        try { s = Api.get("siparis", mapOf("id" to id.toString())).getJSONObject("siparis"); hata = null } catch (e: ApiHata) { hata = e.message }
    }
    LaunchedEffect(id) { yukle() }
    Ekran(s?.optString("no") ?: "Sipariş", altBaslik = s?.let { tipAdi(it.optString("tip")) + " · " + durumAdi(it.optString("durum")) }, geri = geri) {
        val o = s
        when {
            o == null && hata != null -> HataKutusu(hata!!) { kapsam.launch { yukle() } }
            o == null -> Yukleniyor()
            else -> LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
                item {
                    Kart {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(tl(o.optDouble("toplam")), fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                            DurumRozet(o.optString("durum"))
                        }
                        Ayrac()
                        BilgiSatir("Tür", tipAdi(o.optString("tip")))
                        if (o.optString("masa").isNotEmpty()) BilgiSatir("Masa", o.optString("masa"))
                        if (o.optString("kanal").isNotEmpty()) BilgiSatir("Kanal", o.optString("kanal"))
                        BilgiSatir("Açılış", tarihSaat(o.optString("acilis")))
                        BilgiSatir("Kapanış", tarihSaat(o.optString("kapanis")))
                        if (o.optString("kasiyer").isNotEmpty()) BilgiSatir("Kasiyer", o.optString("kasiyer"))
                        if (o.optString("iptal_nedeni").isNotEmpty()) BilgiSatir("İptal nedeni", o.optString("iptal_nedeni"), renk = Kirmizi)
                        if (o.optString("not").isNotEmpty()) Text("📝 " + o.optString("not"), fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp))
                    }
                }
                if (o.optString("musteri").isNotEmpty() || o.optString("telefon").isNotEmpty() || o.optString("adres").isNotEmpty()) item {
                    Kart {
                        BolumBaslik("Müşteri")
                        if (o.optString("musteri").isNotEmpty()) Text(o.optString("musteri"), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        if (o.optString("telefon").isNotEmpty()) Text(o.optString("telefon"), color = Soluk)
                        if (o.optString("adres").isNotEmpty()) Text(o.optString("adres"), fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val tel = o.optString("telefon").filter { it.isDigit() }
                            if (tel.isNotEmpty()) OutlinedButton(onClick = { os.telefonAra(tel) }, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Filled.Call, null, tint = Turuncu); Spacer(Modifier.width(6.dp)); Text("Ara", color = Murekkep)
                            }
                            val hedef = if (!o.isNull("lat")) "${o.optDouble("lat")},${o.optDouble("lng")}" else o.optString("adres")
                            if (hedef.isNotBlank()) OutlinedButton(onClick = { os.haritaAc(hedef) }, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Filled.Place, null, tint = Turuncu); Spacer(Modifier.width(6.dp)); Text("Yol tarifi", color = Murekkep)
                            }
                        }
                    }
                }
                item {
                    Kart {
                        BolumBaslik("Ürünler")
                        o.optJSONArray("kalemler").nesneler().forEach { k ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                                val adet = k.optDouble("adet").let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() }
                                Text("${adet}×", color = Turuncu, fontWeight = FontWeight.Bold, modifier = Modifier.width(36.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(k.optString("ad"), fontWeight = FontWeight.SemiBold)
                                    val ek = (k.optJSONArray("secenek").metinler().filter { it.isNotBlank() }.map { "+ $it" } +
                                        k.optJSONArray("cikar").metinler().filter { it.isNotBlank() }.map { "− $it" }).joinToString(", ")
                                    if (ek.isNotEmpty()) Text(ek, color = Soluk, fontSize = 12.sp)
                                    if (k.optString("not").isNotEmpty()) Text("📝 " + k.optString("not"), color = Soluk, fontSize = 12.sp)
                                }
                                Text(tl(k.optDouble("tutar")), fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Ayrac()
                        BilgiSatir("Ara toplam", tl(o.optDouble("ara_toplam")))
                        if (o.optDouble("indirim") > 0) BilgiSatir("İndirim", "−" + tl(o.optDouble("indirim")), renk = Kirmizi)
                        BilgiSatir("KDV (dahil)", tl(o.optDouble("kdv")))
                        BilgiSatir("Toplam", tl(o.optDouble("toplam")), kalin = true)
                    }
                }
                val odemeler = o.optJSONArray("odemeler").nesneler()
                if (odemeler.isNotEmpty()) item {
                    Kart {
                        BolumBaslik("Ödemeler")
                        odemeler.forEach { p ->
                            BilgiSatir(p.optString("ad"), tl(p.optDouble("tutar")), alt = if (p.optDouble("komisyon") > 0) "Komisyon " + tl(p.optDouble("komisyon")) else null)
                        }
                    }
                }
            }
        }
    }
}



@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.teksosdoner.app.ui

import com.teksosdoner.app.*

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TableRestaurant
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teksosdoner.app.Api
import com.teksosdoner.app.ApiHata
import com.teksosdoner.app.gunAdi
import com.teksosdoner.app.kisaTarih
import com.teksosdoner.app.nesneler
import com.teksosdoner.app.tl
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.teksosdoner.app.json.JSONObject

@Composable
fun PatronEkrani() {
    var veri by remember { mutableStateOf<JSONObject?>(null) }
    var hata by remember { mutableStateOf<String?>(null) }
    var yenileniyor by remember { mutableStateOf(false) }
    var secili by rememberSaveable { mutableStateOf("") }
    val kapsam = rememberCoroutineScope()

    suspend fun yukle() {
        try { veri = Api.get("patron"); hata = null } catch (e: ApiHata) { hata = e.message }
    }
    fun yenile() { kapsam.launch { yenileniyor = true; yukle(); yenileniyor = false } }
    LaunchedEffect(Unit) { while (true) { yukle(); delay(60_000) } }

    Ekran("Özet", altBaslik = veri?.let { "Canlı · son güncelleme " + it.optString("zaman") }, eylemler = {
        IconButton(onClick = { yenile() }) { Icon(Icons.Filled.Refresh, contentDescription = "Yenile") }
    }) {
        val v = veri
        when {
            v == null && hata != null -> HataKutusu(hata!!) { yenile() }
            v == null -> Yukleniyor()
            else -> {
                val subeler = v.optJSONArray("subeler").nesneler()
                val gosterilen = if (subeler.size == 1) subeler.first().optString("kod") else secili
                Column(Modifier.fillMaxSize()) {
                    if (subeler.size > 1) {
                        LazyRow(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item { SubeCip(gosterilen == "", "Tüm şubeler") { secili = "" } }
                            subeler.forEach { s -> item { SubeCip(gosterilen == s.optString("kod"), s.optString("ad")) { secili = s.optString("kod") } } }
                        }
                    }
                    PullToRefreshBox(isRefreshing = yenileniyor, onRefresh = { yenile() }, modifier = Modifier.fillMaxSize()) {
                        LazyColumn(contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = if (subeler.size > 1) 0.dp else 14.dp, bottom = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
                            if (hata != null) item { Serit(Icons.Filled.Warning, hata!!, Kirmizi, KirmiziAcik) }
                            if (gosterilen == "") tumSubeler(subeler) { secili = it }
                            else subeler.firstOrNull { it.optString("kod") == gosterilen }?.let { subeDetay(it) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubeCip(secili: Boolean, ad: String, tikla: () -> Unit) {
    FilterChip(
        selected = secili, onClick = tikla, shape = RoundedCornerShape(999.dp),
        label = { Text(ad, fontWeight = FontWeight.SemiBold) },
        leadingIcon = { Icon(Icons.Filled.Storefront, null, Modifier.size(18.dp)) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Koyu, selectedLabelColor = Color.White, selectedLeadingIconColor = Color(0xFFFDBA74),
            containerColor = Color.White, labelColor = Murekkep, iconColor = Soluk
        )
    )
}

private fun LazyListScope.tumSubeler(subeler: List<JSONObject>, sec: (String) -> Unit) {
    val ciro = subeler.sumOf { it.optJSONObject("bugun")?.optDouble("ciro") ?: 0.0 }
    val adet = subeler.sumOf { it.optJSONObject("bugun")?.optInt("adet") ?: 0 }
    val gh = subeler.sumOf { it.optDouble("gecen_hafta", 0.0) }
    val net = subeler.sumOf { it.optDouble("net", 0.0) }
    val ay = subeler.sumOf { it.optDouble("ay", 0.0) }
    val gAy = subeler.sumOf { it.optDouble("gecen_ay", 0.0) }
    val acik = subeler.sumOf { it.optJSONObject("acik")?.optDouble("tutar") ?: 0.0 } + subeler.sumOf { it.optJSONObject("yp_acik")?.optDouble("tutar") ?: 0.0 }
    item {
        Kart(koyu = true) {
            Text("BUGÜN · TÜM ŞUBELER", color = Color(0xFFA8A29E), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
            Spacer(Modifier.height(6.dp))
            Text(tl(ciro), fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                DegisimRozet(ciro, gh)
                Spacer(Modifier.width(8.dp))
                Text("geçen hafta bu saate göre · $adet satış", color = Color(0xFFD6D3D1), fontSize = 13.sp)
            }
            if (acik > 0) Text("+ ${tl(acik)} açık adisyon (kapanınca ciroya eklenir)", color = Color(0xFFFDBA74), fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
        }
    }
    item {
        KpiIzgara(listOf<@Composable (Modifier) -> Unit>(
            { m -> KpiKutu("Tahmini net kâr", tl(net), Icons.Filled.Savings, if (net >= 0) Yesil else Kirmizi, m, alt = "bugün") },
            { m -> KpiKutu("Bu ay", tl(ay), Icons.Filled.CalendarMonth, Mavi, m, simdi = ay, once = gAy, alt = "geçen aya göre") },
        ))
    }
    item { BolumBaslik("Şubeler", Modifier.padding(start = 4.dp, top = 4.dp)) }
    subeler.forEach { s ->
        item {
            val b = s.optJSONObject("bugun") ?: JSONObject()
            Kart(onClick = { sec(s.optString("kod")) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IkonRozet(Icons.Filled.Storefront, Turuncu, 40)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(s.optString("ad"), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).background(if (s.optBoolean("kasa_acik")) Yesil else Color(0xFFA8A29E), CircleShape))
                            Spacer(Modifier.width(6.dp))
                            Text((if (s.optBoolean("kasa_acik")) "Kasa açık" else "Kasa kapalı") + " · ${b.optInt("adet")} satış · ${s.optJSONArray("mesaide").nesneler().size} kişi mesaide", color = Soluk, fontSize = 12.sp)
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(tl(b.optDouble("ciro")), fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                        DegisimRozet(b.optDouble("ciro"), s.optDouble("gecen_hafta"))
                    }
                }
                val ypd = s.optJSONObject("yp_durum")
                if (ypd != null && ypd.optBoolean("sorun")) {
                    Spacer(Modifier.height(10.dp))
                    Serit(Icons.Filled.Warning, "YemekPOS aktarımında aksama · son ${ypd.optString("saat")}", Kirmizi, KirmiziAcik)
                }
            }
        }
    }
}

private fun LazyListScope.subeDetay(s: JSONObject) {
    if (s.has("hata")) { item { Serit(Icons.Filled.Warning, s.optString("ad") + ": " + s.optString("hata"), Kirmizi, KirmiziAcik) }; return }
    val b = s.getJSONObject("bugun")
    val ciro = b.optDouble("ciro")
    val gh = s.optDouble("gecen_hafta")
    item {
        Kart(koyu = true) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("BUGÜN · " + s.optString("ad").uppercase(com.teksosdoner.app.TR), color = Color(0xFFA8A29E), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, modifier = Modifier.weight(1f))
                Text("iş günü " + kisaTarih(s.optString("gun")), color = Color(0xFFA8A29E), fontSize = 12.sp)
            }
            Spacer(Modifier.height(6.dp))
            Text(tl(ciro), fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                DegisimRozet(ciro, gh)
                Spacer(Modifier.width(8.dp))
                Text("geçen hafta bu saat: ${tl(gh)}", color = Color(0xFFD6D3D1), fontSize = 13.sp)
            }
            val hedef = s.optDouble("hedef_gun")
            if (hedef > 0) {
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(progress = { (ciro / hedef).toFloat().coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(8.dp), color = Color(0xFFFDBA74), trackColor = Koyu2)
                Text("Günlük hedef ${tl(hedef)} · %${(ciro / hedef * 100).toInt()}", color = Color(0xFFD6D3D1), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
    item {
        val acik = s.optJSONObject("acik") ?: JSONObject()
        val net = s.optDouble("net")
        KpiIzgara(listOf<@Composable (Modifier) -> Unit>(
            { m -> KpiKutu("Satış", b.optInt("adet").toString(), Icons.Filled.Receipt, Mavi, m, alt = "dün tam gün ${tl(s.optDouble("dun"))}") },
            { m -> KpiKutu("Ortalama sepet", tl(b.optDouble("sepet")), Icons.Filled.ShoppingBasket, Color(0xFF7C3AED), m) },
            { m -> KpiKutu("Tahmini net kâr", tl(net), Icons.Filled.Savings, if (net >= 0) Yesil else Kirmizi, m, alt = "KDV, maliyet, komisyon, gider sonrası") },
            { m -> KpiKutu("Açık adisyon", tl(acik.optDouble("tutar")), Icons.Filled.TableRestaurant, Sari, m, alt = "${acik.optInt("masa")} masa · ${acik.optInt("paket")} paket") },
        ))
    }
    item {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Rozet(if (s.optBoolean("kasa_acik")) "● Kasa açık" else "○ Kasa kapalı", if (s.optBoolean("kasa_acik")) Yesil else Soluk, if (s.optBoolean("kasa_acik")) YesilAcik else Color(0xFFF5F5F4))
            val yp = s.optJSONObject("yp_acik")
            if (yp != null && yp.optInt("adet") > 0) Rozet("YemekPOS'ta açık ${yp.optInt("adet")} · ${tl(yp.optDouble("tutar"))}", Sari, SariAcik)
            val ypd = s.optJSONObject("yp_durum")
            if (ypd != null) {
                if (ypd.optBoolean("sorun")) Rozet("● YemekPOS aksama · son ${ypd.optString("saat")}", Kirmizi, KirmiziAcik)
                else Rozet("● YemekPOS aktarımı çalışıyor", Yesil, YesilAcik)
            }
            if (s.optInt("online_bekleyen") > 0) Rozet("Online bekleyen ${s.optInt("online_bekleyen")}", Mavi, MaviAcik)
            if (s.optInt("kritik_stok") > 0) Rozet("Kritik stok ${s.optInt("kritik_stok")}", Kirmizi, KirmiziAcik)
            if (s.optInt("maliyetsiz_kalem") > 0) Rozet("${s.optInt("maliyetsiz_kalem")} kalemin maliyeti yok", Sari, SariAcik)
        }
    }
    item {
        Kart {
            KartBaslik(Icons.Filled.CalendarMonth, "Bu ay", "${s.optInt("ay_gecen_gun")}/${s.optInt("ay_gun")} gün", renk = Mavi)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tl(s.optDouble("ay")), fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                DegisimRozet(s.optDouble("ay"), s.optDouble("gecen_ay"))
            }
            Text("Geçen ayın aynı dönemine göre", color = Soluk, fontSize = 12.sp)
            Ayrac()
            BilgiSatir("Bu hızla ay sonu", tl(s.optDouble("ay_tahmin")), kalin = true)
            BilgiSatir("Geçen ay toplam", tl(s.optDouble("gecen_ay_tam")))
            val hedefAy = s.optDouble("hedef_ay")
            if (hedefAy > 0) {
                BilgiSatir("Aylık hedef", tl(hedefAy))
                LinearProgressIndicator(progress = { (s.optDouble("ay") / hedefAy).toFloat().coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(8.dp).padding(top = 2.dp), color = Mavi, trackColor = Color(0xFFF1EDE7))
            }
        }
    }
    val son7 = s.optJSONArray("son7").nesneler()
    if (son7.isNotEmpty()) item {
        Kart {
            KartBaslik(Icons.Filled.BarChart, "Son 7 gün", "bugün turuncu")
            CubukGrafik(son7.map { it.optDouble("t") }, son7.map { gunAdi(it.optString("gun")) }, vurgu = son7.size - 1)
        }
    }
    val saatlik = s.optJSONArray("saatlik").nesneler()
    val dolu = saatlik.indices.filter { saatlik[it].optDouble("t") > 0 || saatlik[it].optDouble("gh") > 0 }
    if (dolu.isNotEmpty()) item {
        val dilim = saatlik.subList(dolu.first(), dolu.last() + 1)
        Kart {
            KartBaslik(Icons.Filled.AccessTime, "Saatlik satış", "gri: geçen hafta")
            CubukGrafik(dilim.map { it.optDouble("t") }, dilim.mapIndexed { i, x -> if (i % 2 == 0) x.optInt("saat").toString() else "" },
                karsi = dilim.map { it.optDouble("gh") }, yukseklik = 110.dp, degerYaz = false)
        }
    }
    val kanal = s.optJSONArray("kanal").nesneler()
    if (kanal.isNotEmpty()) item {
        Kart {
            KartBaslik(Icons.Filled.PointOfSale, "Satış kanalları")
            HalkaGrafik(kanal.mapIndexed { i, k -> Dilim(k.optString("ad"), k.optDouble("t"), Palet[i % Palet.size], "${k.optInt("adet")} · ${tl(k.optDouble("t"))}") }, "Kanal", kanal.size.toString())
        }
    }
    val odeme = s.optJSONArray("odeme").nesneler()
    if (odeme.isNotEmpty()) item {
        Kart {
            KartBaslik(Icons.Filled.Payments, "Ödemeler", renk = Mavi)
            val top = odeme.sumOf { it.optDouble("t") }.let { if (it <= 0) 1.0 else it }
            odeme.forEachIndexed { i, o -> SiraSatir(i + 1, o.optString("ad"), null, tl(o.optDouble("t")), (o.optDouble("t") / top).toFloat(), rozetRenk = Mavi) }
        }
    }
    val enCok = s.optJSONArray("en_cok").nesneler()
    if (enCok.isNotEmpty()) item {
        Kart {
            KartBaslik(Icons.Filled.EmojiEvents, "Bugün en çok satanlar")
            val enBuyuk = enCok.maxOfOrNull { it.optDouble("t") } ?: 1.0
            enCok.forEachIndexed { i, u ->
                val adet = u.optDouble("adet").let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() }
                SiraSatir(i + 1, u.optString("ad"), "$adet adet", tl(u.optDouble("t")), (u.optDouble("t") / enBuyuk).toFloat())
            }
        }
    }
    val mesaide = s.optJSONArray("mesaide").nesneler()
    item {
        Kart {
            KartBaslik(Icons.Filled.Groups, "Şu an mesaide", "${mesaide.size} kişi", renk = Yesil)
            if (mesaide.isEmpty()) Text("Henüz kimse giriş yapmadı.", color = Soluk, fontSize = 14.sp)
            else FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                mesaide.forEach { m ->
                    Row(Modifier.background(Color(0xFFF5F5F4), RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(22.dp).background(Yesil, CircleShape), contentAlignment = Alignment.Center) {
                            Text(m.optString("ad").take(1), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(6.dp))
                        Text(m.optString("ad"), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(" · " + m.optString("giris"), fontSize = 12.sp, color = Soluk)
                    }
                }
            }
        }
    }
}

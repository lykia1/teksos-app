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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teksosdoner.app.Api
import com.teksosdoner.app.ApiHata
import com.teksosdoner.app.TR
import com.teksosdoner.app.gunAdi
import com.teksosdoner.app.kisaTarih
import com.teksosdoner.app.nesneler
import com.teksosdoner.app.tarihSaat
import com.teksosdoner.app.tipAdi
import com.teksosdoner.app.tl
import kotlinx.coroutines.launch
import com.teksosdoner.app.json.JSONObject

private val Donemler = listOf("bugun" to "Bugün", "dun" to "Dün", "hafta" to "Bu hafta", "ay" to "Bu ay", "gecen_ay" to "Geçen ay", "30" to "Son 30 gün")
private val Sekmeler = listOf("Genel", "Ürünler", "Kanallar", "Zaman", "Ekip")
private val HaftaGun = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")

@Composable
fun RaporlarEkrani() {
    val kapsam = rememberCoroutineScope()
    var donem by rememberSaveable { mutableStateOf("bugun") }
    var bas by rememberSaveable { mutableStateOf("") }
    var bit by rememberSaveable { mutableStateOf("") }
    var sekme by rememberSaveable { mutableIntStateOf(0) }
    var veri by remember { mutableStateOf<JSONObject?>(null) }
    var hata by remember { mutableStateOf<String?>(null) }
    var yukleniyor by remember { mutableStateOf(false) }

    suspend fun yukle() {
        yukleniyor = true
        try {
            veri = Api.get("rapor", if (donem == "ozel") mapOf("bas" to bas, "bit" to bit) else mapOf("aralik" to donem))
            hata = null
        } catch (e: ApiHata) {
            hata = e.message
        } finally {
            yukleniyor = false
        }
    }
    LaunchedEffect(donem, bas, bit) { yukle() }

    fun aralikSec() {
        tarihSec("Başlangıç günü") { b1 ->
            tarihSec("Bitiş günü", b1) { b2 -> bas = minOf(b1, b2); bit = maxOf(b1, b2); donem = "ozel" }
        }
    }

    val v = veri
    val altBaslik = v?.let {
        val b = it.optString("bas"); val t = it.optString("bit")
        (if (b == t) kisaTarih(b) else kisaTarih(b) + " – " + kisaTarih(t)) + " · " + (Donemler.firstOrNull { d -> d.first == donem }?.second ?: "Özel aralık")
    }
    Ekran("Raporlar", altBaslik = altBaslik, eylemler = {
        IconButton(onClick = { kapsam.launch { yukle() } }) { Icon(Icons.Filled.Refresh, "Yenile") }
    }) {
        Column(Modifier.fillMaxSize()) {
            LazyRow(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Donemler.forEach { (k, ad) ->
                    item { DonemCip(donem == k, ad) { donem = k } }
                }
                item { DonemCip(donem == "ozel", if (donem == "ozel") kisaTarih(bas) + "–" + kisaTarih(bit) else "Tarih aralığı", ikonlu = true) { aralikSec() } }
            }
            ScrollableTabRow(
                selectedTabIndex = sekme, containerColor = Zemin, contentColor = Turuncu, edgePadding = 14.dp,
                indicator = { p -> TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(p[sekme]), color = Turuncu, height = 3.dp) },
                divider = {}
            ) {
                Sekmeler.forEachIndexed { i, ad ->
                    Tab(selected = sekme == i, onClick = { sekme = i }, text = {
                        Text(ad, fontWeight = if (sekme == i) FontWeight.ExtraBold else FontWeight.SemiBold, color = if (sekme == i) Turuncu else Soluk)
                    })
                }
            }
            when {
                v == null && hata != null -> HataKutusu(hata!!) { kapsam.launch { yukle() } }
                v == null -> Yukleniyor()
                else -> PullToRefreshBox(isRefreshing = yukleniyor, onRefresh = { kapsam.launch { yukle() } }, modifier = Modifier.fillMaxSize()) {
                    LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
                        if (hata != null) item { Serit(Icons.Filled.Warning, hata!!, Kirmizi, KirmiziAcik) }
                        when (sekme) {
                            0 -> genel(v)
                            1 -> urunler(v)
                            2 -> kanallar(v)
                            3 -> zaman(v)
                            else -> ekip(v)
                        }
                        item { Spacer(Modifier.height(10.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun DonemCip(secili: Boolean, ad: String, ikonlu: Boolean = false, tikla: () -> Unit) {
    FilterChip(
        selected = secili, onClick = tikla, shape = RoundedCornerShape(999.dp),
        label = { Text(ad, fontWeight = FontWeight.SemiBold) },
        leadingIcon = if (ikonlu) ({ Icon(Icons.Filled.DateRange, null, Modifier.padding(0.dp)) }) else null,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Koyu, selectedLabelColor = Color.White, selectedLeadingIconColor = Color.White,
            containerColor = Color.White, labelColor = Murekkep
        )
    )
}

private fun androidx.compose.foundation.lazy.LazyListScope.genel(v: JSONObject) {
    val o = v.optJSONObject("ozet") ?: JSONObject()
    val on = v.optJSONObject("onceki_ozet") ?: JSONObject()
    val onceki = v.optJSONObject("onceki")
    item {
        KpiIzgara(listOf<@Composable (Modifier) -> Unit>(
            { m -> KpiKutu("Ciro", tl(o.optDouble("ciro")), Icons.Filled.Payments, Turuncu, m, koyu = true, simdi = o.optDouble("ciro"), once = on.optDouble("ciro"), alt = "önceki döneme göre") },
            { m -> KpiKutu("Net kâr", tl(o.optDouble("net")), Icons.Filled.Savings, if (o.optDouble("net") >= 0) Yesil else Kirmizi, m, simdi = o.optDouble("net"), once = on.optDouble("net")) },
            { m -> KpiKutu("Sipariş", o.optInt("adet").toString(), Icons.Filled.Receipt, Mavi, m, simdi = o.optInt("adet").toDouble(), once = on.optInt("adet").toDouble()) },
            { m -> KpiKutu("Ortalama sepet", tl(o.optDouble("sepet")), Icons.Filled.ShoppingBasket, Color(0xFF7C3AED), m, simdi = o.optDouble("sepet"), once = on.optDouble("sepet")) },
        ))
    }
    if (onceki != null) item {
        Text("Karşılaştırma: önceki eşit dönem (${kisaTarih(onceki.optString("bas"))} – ${kisaTarih(onceki.optString("bit"))})", color = Soluk, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 4.dp))
    }
    val gunluk = v.optJSONArray("gunluk").nesneler()
    if (gunluk.size > 1) item {
        Kart {
            KartBaslik(Icons.Filled.ShowChart, "Günlük ciro", "günlük ort. " + tl(o.optDouble("gunluk_ort")))
            val adim = maxOf(1, gunluk.size / 6)
            CizgiGrafik(
                gunluk.map { it.optDouble("ciro") },
                gunluk.mapIndexed { i, g -> if (i % adim == 0 || i == gunluk.size - 1) (if (gunluk.size <= 7) gunAdi(g.optString("gun")) else kisaTarih(g.optString("gun"))) else "" }
            )
            val enIyi = gunluk.maxByOrNull { it.optDouble("ciro") }
            if (enIyi != null) Text("En iyi gün: ${kisaTarih(enIyi.optString("gun"))} ${gunAdi(enIyi.optString("gun"))} · ${tl(enIyi.optDouble("ciro"))}", color = Soluk, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
        }
    }
    item {
        Kart {
            KartBaslik(Icons.Filled.AccountBalanceWallet, "Kâr / zarar", "maliyet %" + o.optDouble("maliyet_oran"))
            val ciro = o.optDouble("ciro")
            KarSelalesi(
                listOf(
                    Triple("Ciro (KDV dahil)", ciro, false),
                    Triple("KDV", -o.optDouble("kdv"), false),
                    Triple("Ürün maliyeti", -o.optDouble("maliyet"), false),
                    Triple("Brüt kâr", o.optDouble("brut_kar"), true),
                    Triple("Platform / banka komisyonu", -o.optDouble("komisyon"), false),
                    Triple("Giderler", -o.optDouble("gider"), false),
                    Triple("Net kâr", o.optDouble("net"), true),
                ), ciro
            )
            Spacer(Modifier.height(10.dp))
            BilgiSatir("Verilen indirim", tl(o.optDouble("indirim")))
            BilgiSatir("İptal", "${o.optInt("iptal_adet")} adet · ${tl(o.optDouble("iptal_tutar"))}", renk = if (o.optInt("iptal_adet") > 0) Kirmizi else Murekkep)
        }
    }
    val tipler = v.optJSONArray("tipler").nesneler()
    if (tipler.isNotEmpty()) item {
        Kart {
            KartBaslik(Icons.Filled.PieChart, "Satış türü")
            HalkaGrafik(
                tipler.mapIndexed { i, t -> Dilim(tipAdi(t.optString("tip")), t.optDouble("ciro"), Palet[i % Palet.size], "${t.optInt("adet")} sipariş · ${tl(t.optDouble("ciro"))}") },
                "Sipariş", o.optInt("adet").toString()
            )
        }
    }
    if (v.optJSONArray("urunler").nesneler().any { it.optBoolean("maliyetsiz") }) item {
        Serit(Icons.Filled.Info, "Bazı ürünlerin reçetesi/maliyeti yok; kâr olduğundan yüksek görünür. Ürünler sekmesinde işaretli.", Sari, SariAcik)
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.urunler(v: JSONObject) {
    val kat = v.optJSONArray("kategoriler").nesneler()
    val urun = v.optJSONArray("urunler").nesneler()
    if (kat.isNotEmpty()) item {
        Kart {
            KartBaslik(Icons.Filled.Category, "Kategoriler")
            HalkaGrafik(
                kat.mapIndexed { i, k -> Dilim(k.optString("ad"), k.optDouble("ciro"), renkCoz(k.optString("renk"), Palet[i % Palet.size]).let { if (it == Color(0xFF999999)) Palet[i % Palet.size] else it }, tl(k.optDouble("ciro"))) },
                "Kategori", kat.size.toString()
            )
        }
    }
    item {
        Kart {
            KartBaslik(Icons.Filled.Restaurant, "En çok satan ürünler", "ciroya göre")
            if (urun.isEmpty()) Text("Bu dönemde satış yok.", color = Soluk)
            val enBuyuk = urun.maxOfOrNull { it.optDouble("ciro") } ?: 1.0
            urun.take(30).forEachIndexed { i, u ->
                val adet = u.optDouble("adet").let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() }
                SiraSatir(
                    i + 1, u.optString("ad"),
                    listOf(u.optString("kat").ifEmpty { null }, "$adet adet").filterNotNull().joinToString(" · "),
                    tl(u.optDouble("ciro")), (u.optDouble("ciro") / enBuyuk).toFloat(),
                    sagAlt = if (u.optBoolean("maliyetsiz")) "maliyet yok" else "marj %" + String.format(TR, "%.0f", u.optDouble("marj"))
                )
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.kanallar(v: JSONObject) {
    val k = v.optJSONArray("kanallar").nesneler()
    val o = v.optJSONArray("odemeler").nesneler()
    if (k.isNotEmpty()) item {
        Kart {
            KartBaslik(Icons.Filled.Storefront, "Satış kanalları", "net = KDV, maliyet, komisyon sonrası")
            HalkaGrafik(k.mapIndexed { i, x -> Dilim(x.optString("ad"), x.optDouble("ciro"), Palet[i % Palet.size], "${x.optInt("adet")} sipariş") }, "Kanal", k.size.toString())
            Ayrac()
            k.forEach { x ->
                Column(Modifier.padding(vertical = 6.dp)) {
                    Row { Text(x.optString("ad"), Modifier.weight(1f), fontWeight = FontWeight.Bold); Text(tl(x.optDouble("ciro")), fontWeight = FontWeight.Bold) }
                    Row {
                        Text("${x.optInt("adet")} sipariş · komisyon ${tl(x.optDouble("komisyon"))}", Modifier.weight(1f), color = Soluk, fontSize = 12.sp)
                        Text("net " + tl(x.optDouble("net")), color = if (x.optDouble("net") >= 0) Yesil else Kirmizi, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
    if (o.isNotEmpty()) item {
        Kart {
            KartBaslik(Icons.Filled.CreditCard, "Ödeme yöntemleri", renk = Mavi)
            val top = o.sumOf { it.optDouble("tutar") }.let { if (it <= 0) 1.0 else it }
            o.forEachIndexed { i, x ->
                SiraSatir(
                    i + 1, x.optString("ad"), "${x.optInt("adet")} sipariş" + (if (x.optDouble("komisyon") > 0) " · komisyon " + tl(x.optDouble("komisyon")) else ""),
                    tl(x.optDouble("tutar")), (x.optDouble("tutar") / top).toFloat(), sagAlt = "%" + String.format(TR, "%.0f", x.optDouble("tutar") / top * 100), rozetRenk = Mavi
                )
            }
        }
    }
    if (k.isEmpty() && o.isEmpty()) item { BosKutu("Bu dönemde satış yok.") }
}

private fun androidx.compose.foundation.lazy.LazyListScope.zaman(v: JSONObject) {
    val saat = v.optJSONArray("saatlik").nesneler()
    val dolu = saat.indices.filter { saat[it].optDouble("ciro") > 0 }
    if (dolu.isNotEmpty()) item {
        val dilim = saat.subList(dolu.first(), dolu.last() + 1)
        val enIyi = dilim.maxByOrNull { it.optDouble("ciro") }
        Kart {
            KartBaslik(Icons.Filled.Schedule, "Saatlere göre satış", enIyi?.let { "en yoğun " + it.optInt("saat") + ":00" })
            CubukGrafik(dilim.map { it.optDouble("ciro") }, dilim.mapIndexed { i, x -> if (i % 2 == 0) x.optInt("saat").toString() else "" }, degerYaz = dilim.size <= 12)
            Text("Saat başına sipariş sayısı ve ciro; mutfak ve kurye planlaması için kullanın.", color = Soluk, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
        }
    }
    val hg = v.optJSONArray("hafta_gunu").nesneler()
    if (hg.any { it.optDouble("ciro") > 0 }) item {
        val enIyi = hg.indices.maxByOrNull { hg[it].optDouble("ciro") } ?: 0
        Kart {
            KartBaslik(Icons.Filled.DateRange, "Haftanın günleri", "en iyi: " + HaftaGun[enIyi])
            CubukGrafik(hg.map { it.optDouble("ciro") }, HaftaGun)
            Spacer(Modifier.height(8.dp))
            hg.forEachIndexed { i, g ->
                if (g.optInt("adet") > 0) BilgiSatir(HaftaGun[i], "${g.optInt("adet")} sipariş · ${tl(g.optDouble("ciro"))}")
            }
        }
    }
    val o = v.optJSONObject("ozet") ?: JSONObject()
    if (o.optDouble("mesai_saat") > 0) item {
        Kart {
            KartBaslik(Icons.Filled.TrendingUp, "Verimlilik", renk = Yesil)
            BilgiSatir("Toplam mesai", String.format(TR, "%.1f saat", o.optDouble("mesai_saat")))
            BilgiSatir("Mesai saati başına ciro", tl(o.optDouble("saat_ciro")), kalin = true)
            BilgiSatir("Online sipariş", o.optInt("online").toString())
        }
    }
    if (dolu.isEmpty()) item { BosKutu("Bu dönemde satış yok.") }
}

private fun androidx.compose.foundation.lazy.LazyListScope.ekip(v: JSONObject) {
    val p = v.optJSONArray("personel").nesneler()
    val k = v.optJSONArray("kuryeler").nesneler()
    val ip = v.optJSONArray("iptaller").nesneler()
    if (p.isNotEmpty()) item {
        Kart {
            KartBaslik(Icons.Filled.Groups, "Kasada satış (kapatan kişi)", renk = Mavi)
            val enBuyuk = p.maxOfOrNull { it.optDouble("ciro") } ?: 1.0
            p.forEachIndexed { i, x ->
                SiraSatir(
                    i + 1, x.optString("ad").let { if (it == "—") "YemekPOS / otomatik" else it },
                    "${x.optInt("adet")} satış" + (if (x.optDouble("indirim") > 0) " · indirim " + tl(x.optDouble("indirim")) else ""),
                    tl(x.optDouble("ciro")), (x.optDouble("ciro") / enBuyuk).toFloat(), rozetRenk = Mavi
                )
            }
        }
    }
    if (k.isNotEmpty()) item {
        Kart {
            KartBaslik(Icons.Filled.DeliveryDining, "Kuryeler", renk = Yesil)
            val enBuyuk = k.maxOfOrNull { it.optInt("adet").toDouble() } ?: 1.0
            k.forEachIndexed { i, x ->
                SiraSatir(
                    i + 1, x.optString("ad"), "${tl(x.optDouble("ciro"))} tutarında paket",
                    "${x.optInt("adet")} paket", (x.optInt("adet") / enBuyuk).toFloat(),
                    sagAlt = if (!x.isNull("ort_dk") && x.optInt("ort_dk") > 0) "ort. ${x.optInt("ort_dk")} dk yolda" else null, rozetRenk = Yesil
                )
            }
        }
    }
    item {
        Kart {
            KartBaslik(Icons.Filled.Cancel, "İptaller", "${ip.size} kayıt", renk = Kirmizi)
            if (ip.isEmpty()) Text("Bu dönemde iptal yok 👍", color = Soluk)
            ip.forEach { x ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(x.optString("no") + " · " + tarihSaat(x.optString("zaman")), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(listOf(x.optString("neden").ifEmpty { null }, x.optString("kisi").ifEmpty { null }).filterNotNull().joinToString(" · ").ifEmpty { "Neden yazılmamış" }, color = Soluk, fontSize = 12.sp)
                    }
                    Text(tl(x.optDouble("tutar")), color = Kirmizi, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

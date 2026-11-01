@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)

package com.teksosdoner.app.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TableRestaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teksosdoner.app.Api
import com.teksosdoner.app.ApiHata
import com.teksosdoner.app.Oturum
import com.teksosdoner.app.TR
import com.teksosdoner.app.Yazdir
import com.teksosdoner.app.nesneler
import com.teksosdoner.app.tarihSaat
import com.teksosdoner.app.tl
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/* ============================== VERİ ============================== */

data class Secim(val id: Int, val ad: String, val fiyat: Double, val slot: String)

data class Kalem(
    val anahtar: String = UUID.randomUUID().toString(),
    val urunId: Int, val ad: String, val adet: Double, val taban: Double,
    val secimler: List<Secim> = emptyList(), val cikar: List<String> = emptyList(), val not: String = "",
) {
    val birim: Double get() = taban + secimler.sumOf { it.fiyat }
    val tutar: Double get() = birim * adet
    fun json(): JSONObject = JSONObject().put("product_id", urunId).put("qty", adet).put("note", not)
        .put("options", JSONArray(secimler.map { JSONObject().put("id", it.id).put("slot", it.slot) }))
        .put("removed", JSONArray(cikar))
}

/** Kasanın açılışta aldığı menü + yardımcı eşlemeler. */
class KasaVeri(val j: JSONObject) {
    val urunler: List<JSONObject> = j.optJSONArray("products").nesneler()
    val urun: Map<Int, JSONObject> = urunler.associateBy { it.optInt("id") }
    val kategoriler: List<JSONObject> = j.optJSONArray("categories").nesneler()
    val katRenk: Map<Int, String> = kategoriler.associate { it.optInt("id") to it.optString("color") }
    val grup: Map<Int, JSONObject> = j.optJSONArray("option_groups").nesneler().associateBy { it.optInt("id") }
    val yontemler: List<JSONObject> = j.optJSONArray("methods").nesneler()
    val kanallar: List<JSONObject> = j.optJSONArray("channels").nesneler()
    val giderKat: List<JSONObject> = j.optJSONArray("expense_categories").nesneler()
    val altGrup: JSONObject = j.optJSONObject("sub_groups") ?: JSONObject()
    val admin: Boolean = j.optJSONObject("user")?.optString("role") == "admin"
    fun fiyat(p: JSONObject?, tip: String): Double = p?.optJSONObject("prices")?.optDouble(tip, p.optDouble("price")) ?: 0.0
    fun altGruplar(urunId: Int): List<Int> = altGrup.optJSONArray(urunId.toString())?.let { a -> (0 until a.length()).map { a.optInt(it) } } ?: emptyList()
}

/** Ekranda düzenlenen adisyon. */
class Adisyon {
    var tip by mutableStateOf("takeaway")
    var id by mutableIntStateOf(0)
    var no by mutableStateOf("")
    var masaId by mutableIntStateOf(0)
    var masaAd by mutableStateOf("")
    var ad by mutableStateOf("")
    var tel by mutableStateOf("")
    var adres by mutableStateOf("")
    var kanal by mutableIntStateOf(0)
    var not by mutableStateOf("")
    val kalemler = mutableStateListOf<Kalem>()
    val ara: Double get() = kalemler.sumOf { it.tutar }
    val adet: Int get() = kalemler.sumOf { it.adet }.toInt()

    fun sifirla(yeniTip: String = "takeaway") {
        tip = yeniTip; id = 0; no = ""; masaId = 0; masaAd = ""; ad = ""; tel = ""; adres = ""; kanal = 0; not = ""; kalemler.clear()
    }

    fun govde(ek: JSONObject.() -> Unit = {}): JSONObject = JSONObject().apply {
        if (id > 0) put("order_id", id)
        put("type", tip)
        if (tip == "table") put("table_id", masaId)
        put("items", JSONArray(kalemler.map { it.json() }))
        put("customer", JSONObject().put("name", ad).put("phone", tel).put("address", adres))
        if (tip == "delivery" && kanal > 0) put("channel_id", kanal)
        put("note", not)
        ek()
    }

    fun yukle(o: JSONObject, v: KasaVeri) {
        sifirla(o.optString("type", "takeaway"))
        id = o.optInt("id"); no = o.optString("order_no")
        masaId = o.optInt("table_id"); masaAd = o.optString("table_name").let { if (it == "null") "" else it }
        ad = o.optString("customer_name").let { if (it == "null") "" else it }
        tel = o.optString("customer_phone").let { if (it == "null") "" else it }
        adres = o.optString("customer_address").let { if (it == "null") "" else it }
        kanal = o.optInt("channel_id"); not = o.optString("note").let { if (it == "null") "" else it }
        o.optJSONArray("items").nesneler().forEach { i ->
            val sec = i.optJSONArray("options").nesneler().map { Secim(it.optInt("id"), it.optString("name"), it.optDouble("price"), it.optString("slot")) }
            val cik = i.optJSONArray("removed")?.let { a -> (0 until a.length()).map { a.optString(it) } } ?: emptyList()
            kalemler.add(Kalem(urunId = i.optInt("product_id"), ad = i.optString("name"), adet = i.optDouble("qty"),
                taban = i.optDouble("price") - sec.sumOf { it.fiyat }, secimler = sec, cikar = cik, not = i.optString("note").let { if (it == "null") "" else it }))
        }
    }

    fun tipDegis(yeni: String, v: KasaVeri) {
        tip = yeni
        val eski = kalemler.toList(); kalemler.clear()
        eski.forEach { k -> kalemler.add(k.copy(taban = v.urun[k.urunId]?.let { v.fiyat(it, yeni) } ?: k.taban)) }
    }
}

private fun para(d: Double) = String.format(java.util.Locale.US, "%.2f", d)
private val TipAd = mapOf("takeaway" to "Al götür", "table" to "Masa", "delivery" to "Paket")

/* ============================== EKRAN ============================== */

@Composable
fun KasaEkrani(geri: (() -> Unit)?, siparisAc: (Int) -> Unit) {
    val kapsam = rememberCoroutineScope()
    val ctx = LocalContext.current
    var veri by remember { mutableStateOf<KasaVeri?>(null) }
    var hata by remember { mutableStateOf<String?>(null) }
    var mesaj by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    var durum by remember { mutableStateOf(JSONObject()) }
    var kasaDurum by remember { mutableStateOf<JSONObject?>(null) }
    var gelen by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var sekme by rememberSaveable { mutableIntStateOf(0) }
    var pencere by remember { mutableStateOf<(@Composable () -> Unit)?>(null) }
    var menuAcik by remember { mutableStateOf(false) }
    var tukenen by remember { mutableStateOf<Set<Int>>(emptySet()) }
    val ad = remember { Adisyon() }
    val kapat = { pencere = null }

    suspend fun durumYenile() {
        runCatching { durum = Api.kasa("status") }
        runCatching { kasaDurum = Api.kasa("cash_status") }
    }
    suspend fun yukle() {
        try {
            val b = Api.kasa("bootstrap")
            veri = KasaVeri(b); hata = null
            durum = b
            tukenen = b.optJSONArray("products").nesneler().filter { it.optBoolean("sold_out") }.map { it.optInt("id") }.toSet()
            kasaDurum = Api.kasa("cash_status")
        } catch (e: ApiHata) { hata = e.message }
    }
    fun islem(blok: suspend () -> String?) {
        kapsam.launch {
            try { blok()?.let { mesaj = it to false } } catch (e: ApiHata) { mesaj = (e.message ?: "Hata") to true }
            durumYenile()
        }
    }

    LaunchedEffect(Unit) { yukle() }
    LaunchedEffect(veri != null) {
        while (veri != null) {
            runCatching { gelen = Api.kasa("incoming").optJSONArray("orders").nesneler() }
            delay(25_000)
        }
    }

    val v = veri
    Ekran("Kasa", altBaslik = when {
        ad.id > 0 -> TipAd[ad.tip] + (if (ad.masaAd.isNotEmpty()) " · " + ad.masaAd else "") + " · #" + ad.no
        else -> kasaDurum?.optJSONObject("session")?.let { "Kasa açık · beklenen nakit " + tl(it.optDouble("expected")) } ?: "Yeni satış"
    }, geri = geri, eylemler = {
        if (gelen.isNotEmpty()) BadgedBox(badge = { Badge { Text(gelen.size.toString()) } }, modifier = Modifier.padding(end = 8.dp).clickable { sekme = 3 }) {
            Icon(Icons.Filled.Inbox, "Gelen siparişler")
        }
        IconButton(onClick = { kapsam.launch { yukle() } }) { Icon(Icons.Filled.Refresh, "Yenile") }
        Box {
            IconButton(onClick = { menuAcik = true }) { Icon(Icons.Filled.MoreVert, "Menü") }
            DropdownMenu(expanded = menuAcik, onDismissRequest = { menuAcik = false }) {
                DropdownMenuItem(text = { Text("Kasadan gider öde") }, onClick = { menuAcik = false; if (v != null) pencere = { GiderPenceresi(v, kapat) { g -> pencere = null; islem { Api.kasa("expense_add", g); "Gider kaydedildi." } } } })
                kasaDurum?.optJSONObject("session")?.let { s ->
                    DropdownMenuItem(text = { Text("Kasayı kapat (sayım)") }, onClick = { menuAcik = false; pencere = { KasaKapatPenceresi(s, kapat) { g -> pencere = null; islem { val r = Api.kasa("cash_close", g); "Kasa kapandı · fark " + tl(r.optDouble("diff")) } } } })
                }
            }
        }
    }) {
        when {
            v == null && hata != null -> HataKutusu(hata!!) { kapsam.launch { yukle() } }
            v == null -> Yukleniyor()
            kasaDurum?.optBoolean("required") == true && kasaDurum?.isNull("session") == true ->
                KasaAcilis(kasaDurum!!) { tutar -> islem { Api.kasa("cash_open", JSONObject().put("amount", tutar)); "Kasa açıldı." } }
            else -> Column(Modifier.fillMaxSize()) {
                ScrollableTabRow(selectedTabIndex = sekme, edgePadding = 8.dp, containerColor = Color.White, contentColor = Turuncu) {
                    listOf("Satış" + if (ad.adet > 0) " (${ad.adet})" else "", "Masalar", "Paket · Al götür", "Gelen" + if (gelen.isNotEmpty()) " (${gelen.size})" else "", "Bugün").forEachIndexed { i, t ->
                        Tab(selected = sekme == i, onClick = { sekme = i }, text = { Text(t, fontWeight = if (sekme == i) FontWeight.Bold else FontWeight.Normal, maxLines = 1) })
                    }
                }
                MesajSeridi(mesaj?.first, mesaj?.second ?: false) { mesaj = null }
                when (sekme) {
                    0 -> SatisGorunumu(v, ad, tukenen,
                        tukendiDegis = { p ->
                            val yeni = p.optInt("id") !in tukenen
                            pencere = { OnayPenceresi(p.optString("name"), if (yeni) "“Tükendi” olarak işaretlensin mi? Kasada ve TV’de satışa kapanır." else "Tekrar satışa açılsın mı?", if (yeni) "Tükendi" else "Satışa aç", tehlikeli = yeni, kapat = kapat) {
                                pencere = null
                                tukenen = if (yeni) tukenen + p.optInt("id") else tukenen - p.optInt("id")
                                islem { Api.kasa("sold_out", JSONObject().put("product_id", p.optInt("id")).put("value", yeni)); null }
                            } }
                        },
                        ac = { pencere = it }, kapat = kapat,
                        kaydet = {
                            islem {
                                val r = Api.kasa("save", ad.govde())
                                val o = r.optJSONObject("order") ?: JSONObject()
                                val mesajMetin = (TipAd[ad.tip] ?: "") + " adisyonu kaydedildi · #" + o.optString("order_no")
                                ad.sifirla(); sekme = if (o.optString("type") == "table") 1 else 2
                                mesajMetin
                            }
                        },
                        odeme = {
                            pencere = {
                                OdemePenceresi(v, ad, kapat) { siparis ->
                                    pencere = null
                                    val no = siparis.optString("order_no"); val sid = siparis.optInt("id")
                                    ad.sifirla()
                                    mesaj = "Satış tamamlandı · #$no · " + tl(siparis.optDouble("total")) to false
                                    if (v.j.optBoolean("auto_print")) Yazdir.fis(ctx, sid)
                                    kapsam.launch { durumYenile() }
                                }
                            }
                        },
                        iptal = {
                            pencere = { NedenPenceresi("Adisyon iptal", "#" + ad.no + " iptal edilecek.", kapat) { neden ->
                                pencere = null
                                islem { Api.kasa("cancel", JSONObject().put("order_id", ad.id).put("reason", neden)); ad.sifirla(); "Adisyon iptal edildi." }
                            } }
                        },
                        masaTasi = {
                            pencere = { MasaSecPenceresi(durum, ad.masaId, kapat) { hedef ->
                                pencere = null
                                islem {
                                    val r = Api.kasa("move_table", JSONObject().put("order_id", ad.id).put("table_id", hedef))
                                    r.optJSONObject("order")?.let { ad.yukle(it, v) }
                                    if (r.optBoolean("merged")) "Adisyonlar birleştirildi." else "Masa taşındı."
                                }
                            } }
                        },
                        yazdir = { Yazdir.fis(ctx, ad.id) },
                        mutfak = { Yazdir.fis(ctx, ad.id, mutfak = true) },
                    )
                    1 -> MasalarGorunumu(durum) { t ->
                        if (t.optInt("order_id") > 0) islem { ad.yukle(Api.kasa("order", null, mapOf("id" to t.optInt("order_id").toString())).optJSONObject("order")!!, v); sekme = 0; null }
                        else { ad.sifirla("table"); ad.masaId = t.optInt("id"); ad.masaAd = t.optString("name"); sekme = 0 }
                    }
                    2 -> PaketGorunumu(durum,
                        ac = { id -> islem { ad.yukle(Api.kasa("order", null, mapOf("id" to id.toString())).optJSONObject("order")!!, v); sekme = 0; null } },
                        durumDegis = { id, d -> islem { Api.kasa("delivery_status", JSONObject().put("order_id", id).put("status", d)); null } },
                        yeni = { t -> ad.sifirla(t); sekme = 0 })
                    3 -> GelenGorunumu(gelen,
                        kabul = { id -> islem { val r = Api.kasa("incoming_accept", JSONObject().put("id", id)); gelen = gelen.filter { it.optInt("id") != id }
                            if (r.optBoolean("auto_print")) r.optJSONObject("order")?.let { Yazdir.fis(ctx, it.optInt("id"), mutfak = true) }; "Sipariş kabul edildi, adisyon açıldı." } },
                        red = { o -> pencere = { NedenPenceresi("Siparişi reddet", o.optString("platform_name") + " · " + o.optString("customer_name"), kapat) { n ->
                            pencere = null; islem { Api.kasa("incoming_reject", JSONObject().put("id", o.optInt("id")).put("reason", n)); gelen = gelen.filter { it.optInt("id") != o.optInt("id") }; "Sipariş reddedildi." } } } })
                    else -> BugunGorunumu(siparisAc, yazdir = { Yazdir.fis(ctx, it) })
                }
            }
        }
    }
    pencere?.invoke()
}

/* ============================== KASA AÇILIŞ ============================== */

@Composable
private fun KasaAcilis(d: JSONObject, ac: (Double) -> Unit) {
    var tutar by remember { mutableStateOf(para(d.optDouble("suggest")).replace('.', ',')) }
    Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
        Kart(Modifier.widthIn(max = 420.dp)) {
            IkonRozet(Icons.Filled.Lock, Turuncu, 52)
            Spacer(Modifier.height(12.dp))
            Text("Kasa kapalı", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
            Text("Satışa başlamadan önce çekmecedeki nakdi sayıp yazın.", color = Soluk)
            d.optJSONObject("last")?.let { Text("Son kapanış: " + tarihSaat(it.optString("closed_at")) + " · sayılan " + tl(it.optDouble("counted_cash")), color = Soluk, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp)) }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(tutar, { tutar = it.filter { c -> c.isDigit() || c == ',' || c == '.' } }, label = { Text("Açılış nakdi (₺)") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), colors = alanRenkleri(), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            Button(onClick = { ac(tutar.replace(',', '.').toDoubleOrNull() ?: 0.0) }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = Turuncu)) {
                Text("Kasayı aç", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

/* ============================== SATIŞ ============================== */

@Composable
private fun SatisGorunumu(
    v: KasaVeri, ad: Adisyon, tukenen: Set<Int>, tukendiDegis: (JSONObject) -> Unit,
    ac: ((@Composable () -> Unit)) -> Unit, kapat: () -> Unit,
    kaydet: () -> Unit, odeme: () -> Unit, iptal: () -> Unit, masaTasi: () -> Unit, yazdir: () -> Unit, mutfak: () -> Unit,
) {
    var kat by rememberSaveable { mutableIntStateOf(0) }
    var ara by rememberSaveable { mutableStateOf("") }
    var sepetAcik by remember { mutableStateOf(false) }
    val urunler = remember(kat, ara, v) {
        v.urunler.filter { p -> (kat == 0 || p.optInt("category_id") == kat) && (ara.isBlank() || p.optString("name").contains(ara.trim(), ignoreCase = true) || p.optString("sku") == ara.trim()) }
    }
    fun ekle(p: JSONObject) {
        val gruplar = p.optJSONArray("groups")?.length() ?: 0
        val icerik = p.optJSONArray("ingredients")?.length() ?: 0
        if (gruplar > 0 || icerik > 0) ac { SecenekPenceresi(v, p, ad.tip, null, kapat) { k -> ad.kalemler.add(k); kapat() } }
        else {
            val i = ad.kalemler.indexOfFirst { it.urunId == p.optInt("id") && it.secimler.isEmpty() && it.cikar.isEmpty() && it.not.isEmpty() }
            if (i >= 0) ad.kalemler[i] = ad.kalemler[i].let { it.copy(adet = it.adet + 1) }
            else ad.kalemler.add(Kalem(urunId = p.optInt("id"), ad = p.optString("name"), adet = 1.0, taban = v.fiyat(p, ad.tip)))
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val genis = maxWidth >= 760.dp
        Row(Modifier.fillMaxSize()) {
            Column(Modifier.weight(1f).fillMaxHeight()) {
                SiparisBasligi(v, ad)
                LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item { Cip2(kat == 0, "Tümü") { kat = 0 } }
                    items(v.kategoriler, key = { it.optInt("id") }) { c -> Cip2(kat == c.optInt("id"), c.optString("name")) { kat = c.optInt("id") } }
                }
                OutlinedTextField(ara, { ara = it }, placeholder = { Text("Ürün ara / barkod") }, singleLine = true, leadingIcon = { Icon(Icons.Filled.Search, null) },
                    trailingIcon = { if (ara.isNotEmpty()) IconButton(onClick = { ara = "" }) { Icon(Icons.Filled.Close, "Temizle") } },
                    shape = RoundedCornerShape(14.dp), colors = alanRenkleri(), modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp))
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(if (genis) 132.dp else 104.dp),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = if (genis) 16.dp else 96.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)
                ) {
                    gridItems(urunler, key = { it.optInt("id") }) { p ->
                        val yok = p.optInt("id") in tukenen
                        val renk = renkCoz(v.katRenk[p.optInt("category_id")], Turuncu)
                        val sepette = ad.kalemler.filter { it.urunId == p.optInt("id") }.sumOf { it.adet }
                        Box(
                            Modifier.height(if (genis) 96.dp else 88.dp).background(if (yok) Color(0xFFF5F5F4) else Color.White, RoundedCornerShape(14.dp))
                                .border(if (sepette > 0) 2.dp else 1.dp, if (sepette > 0) Turuncu else Cizgi, RoundedCornerShape(14.dp))
                                .combinedClickable(onClick = { if (!yok) ekle(p) }, onLongClick = { tukendiDegis(p) })
                        ) {
                            Box(Modifier.padding(start = 8.dp, top = 8.dp).size(width = 22.dp, height = 4.dp).background(renk, RoundedCornerShape(2.dp)))
                            Column(Modifier.fillMaxSize().padding(start = 9.dp, end = 9.dp, top = 16.dp, bottom = 8.dp), verticalArrangement = Arrangement.SpaceBetween) {
                                Text(p.optString("name"), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 15.sp, maxLines = 3, overflow = TextOverflow.Ellipsis, color = if (yok) Soluk else Murekkep)
                                Text(if (yok) "TÜKENDİ" else tl(v.fiyat(p, ad.tip)), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = if (yok) Kirmizi else Murekkep)
                            }
                            if (sepette > 0) Box(Modifier.align(Alignment.TopEnd).padding(6.dp).size(22.dp).background(Turuncu, CircleShape), contentAlignment = Alignment.Center) {
                                Text(sepette.toInt().toString(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            if (genis) Box(Modifier.width(370.dp).fillMaxHeight().background(Color.White)) {
                SepetPaneli(v, ad, ac, kapat, kaydet, odeme, iptal, masaTasi, yazdir, mutfak)
            }
        }
        if (!genis) {
            if (ad.kalemler.isNotEmpty()) Button(
                onClick = { sepetAcik = true }, colors = ButtonDefaults.buttonColors(containerColor = Koyu), shape = RoundedCornerShape(18.dp),
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(12.dp).height(60.dp)
            ) {
                Icon(Icons.Filled.ShoppingCart, null); Spacer(Modifier.width(10.dp))
                Text("${ad.adet} ürün", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text(tl(ad.ara), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            }
            if (sepetAcik) ModalBottomSheet(onDismissRequest = { sepetAcik = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = Color.White) {
                Box(Modifier.fillMaxHeight(0.92f)) {
                    SepetPaneli(v, ad, ac, kapat, kaydet = { sepetAcik = false; kaydet() }, odeme = { sepetAcik = false; odeme() }, iptal = { sepetAcik = false; iptal() },
                        masaTasi = { sepetAcik = false; masaTasi() }, yazdir = yazdir, mutfak = mutfak)
                }
            }
        }
    }
}

@Composable
private fun SiparisBasligi(v: KasaVeri, ad: Adisyon) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
        if (ad.id == 0) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf("takeaway", "table", "delivery").forEachIndexed { i, t ->
                    SegmentedButton(selected = ad.tip == t, onClick = { if (t != "table") { ad.masaId = 0; ad.masaAd = "" }; ad.tipDegis(t, v) },
                        shape = SegmentedButtonDefaults.itemShape(i, 3),
                        colors = SegmentedButtonDefaults.colors(activeContainerColor = TuruncuAcik, activeContentColor = Turuncu)) {
                        Text(TipAd[t] + if (t == "table" && ad.masaAd.isNotEmpty()) " · " + ad.masaAd else "", maxLines = 1)
                    }
                }
            }
            if (ad.tip == "table" && ad.masaId == 0) Text("Masalar sekmesinden bir masa seçin.", color = Sari, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        }
        if (ad.tip == "delivery") MusteriAlani(v, ad)
    }
}

@Composable
private fun MusteriAlani(v: KasaVeri, ad: Adisyon) {
    var oneriler by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    LaunchedEffect(ad.tel) {
        val t = ad.tel.filter { it.isDigit() }
        if (t.length >= 4 && ad.id == 0) { delay(350); oneriler = runCatching { Api.kasa("customer_lookup", null, mapOf("phone" to t)).optJSONArray("customers").nesneler() }.getOrDefault(emptyList()) }
        else oneriler = emptyList()
    }
    Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(ad.tel, { ad.tel = it }, label = { Text("Telefon") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                colors = alanRenkleri(), modifier = Modifier.weight(1f))
            OutlinedTextField(ad.ad, { ad.ad = it }, label = { Text("Ad") }, singleLine = true, colors = alanRenkleri(), modifier = Modifier.weight(1f))
        }
        oneriler.filter { it.optString("phone") != ad.tel }.take(4).forEach { c ->
            Row(Modifier.fillMaxWidth().background(MaviAcik, RoundedCornerShape(10.dp)).clickable {
                ad.tel = c.optString("phone"); ad.ad = c.optString("name"); if (c.optString("address").isNotEmpty() && c.optString("address") != "null") ad.adres = c.optString("address"); oneriler = emptyList()
            }.padding(horizontal = 10.dp, vertical = 6.dp)) {
                Text(c.optString("name") + " · " + c.optString("phone"), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Text("${c.optInt("order_count")} sipariş", color = Soluk, fontSize = 12.sp)
            }
        }
        OutlinedTextField(ad.adres, { ad.adres = it }, label = { Text("Adres (harita linki de olur)") }, minLines = 2, colors = alanRenkleri(), modifier = Modifier.fillMaxWidth())
        if (v.kanallar.isNotEmpty()) LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item { Cip2(ad.kanal == 0, "Telefon / kendi") { ad.kanal = 0 } }
            items(v.kanallar) { c -> Cip2(ad.kanal == c.optInt("id"), c.optString("name")) { ad.kanal = c.optInt("id") } }
        }
    }
}

@Composable
private fun SepetPaneli(
    v: KasaVeri, ad: Adisyon, ac: ((@Composable () -> Unit)) -> Unit, kapat: () -> Unit,
    kaydet: () -> Unit, odeme: () -> Unit, iptal: () -> Unit, masaTasi: () -> Unit, yazdir: () -> Unit, mutfak: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(if (ad.id > 0) "Adisyon #" + ad.no else "Sepet", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                Text(TipAd[ad.tip] + (if (ad.masaAd.isNotEmpty()) " · " + ad.masaAd else "") + (if (ad.ad.isNotEmpty()) " · " + ad.ad else ""), color = Soluk, fontSize = 12.sp)
            }
            if (ad.id > 0) {
                IconButton(onClick = yazdir) { Icon(Icons.Filled.Print, "Adisyon yazdır") }
                if (ad.tip == "table") IconButton(onClick = masaTasi) { Icon(Icons.Filled.TableRestaurant, "Masa taşı") }
                IconButton(onClick = { ad.sifirla() }) { Icon(Icons.Filled.Close, "Adisyonu kapat (yeni satış)") }
            } else if (ad.kalemler.isNotEmpty()) IconButton(onClick = { ad.kalemler.clear() }) { Icon(Icons.Filled.Delete, "Sepeti boşalt", tint = Soluk) }
        }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (ad.kalemler.isEmpty()) item { Text("Sepet boş. Ürüne dokunarak ekleyin.", color = Soluk, modifier = Modifier.padding(16.dp)) }
            items(ad.kalemler, key = { it.anahtar }) { k ->
                Row(Modifier.fillMaxWidth().background(Zemin, RoundedCornerShape(12.dp)).clickable {
                    val p = v.urun[k.urunId]
                    if (p != null && ((p.optJSONArray("groups")?.length() ?: 0) > 0 || (p.optJSONArray("ingredients")?.length() ?: 0) > 0))
                        ac { SecenekPenceresi(v, p, ad.tip, k, kapat) { yeni -> val i = ad.kalemler.indexOfFirst { it.anahtar == k.anahtar }; if (i >= 0) ad.kalemler[i] = yeni.copy(anahtar = k.anahtar); kapat() } }
                    else ac { FormPenceresi(k.ad, listOf(Alan("not", "Not (mutfağa)", deger = k.not)), kapat = kapat) { m -> val i = ad.kalemler.indexOfFirst { it.anahtar == k.anahtar }; if (i >= 0) ad.kalemler[i] = k.copy(not = m["not"].orEmpty()); kapat() } }
                }.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(k.ad, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        val alt = k.secimler.map { it.ad + if (it.fiyat > 0) " +" + tl(it.fiyat) else "" } + k.cikar.map { "−$it" } + listOfNotNull(k.not.ifEmpty { null }?.let { "📝 $it" })
                        if (alt.isNotEmpty()) Text(alt.joinToString(" · "), color = Soluk, fontSize = 11.sp, lineHeight = 13.sp)
                        Text(tl(k.tutar), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    IconButton(onClick = { val i = ad.kalemler.indexOfFirst { it.anahtar == k.anahtar }; if (i >= 0) { if (k.adet <= 1) ad.kalemler.removeAt(i) else ad.kalemler[i] = k.copy(adet = k.adet - 1) } }, modifier = Modifier.size(36.dp)) {
                        Icon(if (k.adet <= 1) Icons.Filled.Delete else Icons.Filled.Remove, "Azalt", tint = Soluk)
                    }
                    Text(if (k.adet % 1.0 == 0.0) k.adet.toInt().toString() else String.format(TR, "%.2f", k.adet), fontWeight = FontWeight.ExtraBold, modifier = Modifier.widthIn(min = 22.dp), textAlign = TextAlign.Center)
                    IconButton(onClick = { val i = ad.kalemler.indexOfFirst { it.anahtar == k.anahtar }; if (i >= 0) ad.kalemler[i] = k.copy(adet = k.adet + 1) }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.Add, "Artır", tint = Turuncu)
                    }
                }
            }
            if (ad.kalemler.isNotEmpty()) item {
                OutlinedTextField(ad.not, { ad.not = it }, label = { Text("Adisyon notu") }, singleLine = true, colors = alanRenkleri(), modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
            }
        }
        Column(Modifier.fillMaxWidth().background(Color.White).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Toplam", color = Soluk, modifier = Modifier.weight(1f))
                Text(tl(ad.ara), fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
            }
            val bos = ad.kalemler.isEmpty() || (ad.tip == "table" && ad.masaId == 0)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (ad.tip != "takeaway" || ad.id > 0) OutlinedButton(onClick = kaydet, enabled = !bos, modifier = Modifier.weight(1f).height(54.dp), shape = RoundedCornerShape(14.dp)) {
                    Text(if (ad.id > 0) "Güncelle" else "Kaydet", fontWeight = FontWeight.Bold, color = if (bos) Soluk else Murekkep)
                }
                Button(onClick = odeme, enabled = !bos, modifier = Modifier.weight(1.4f).height(54.dp), shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Yesil)) { Text("Ödeme al", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp) }
            }
            if (ad.id > 0) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = mutfak, modifier = Modifier.weight(1f)) { Text("Mutfak fişi", color = Mavi) }
                TextButton(onClick = iptal, modifier = Modifier.weight(1f)) { Text("Adisyonu iptal et", color = Kirmizi) }
            }
        }
    }
}

/* ============================== SEÇENEKLER ============================== */

@Composable
private fun SecenekPenceresi(v: KasaVeri, p: JSONObject, tip: String, mevcut: Kalem?, kapat: () -> Unit, tamam: (Kalem) -> Unit) {
    val pid = p.optInt("id")
    val gruplar = p.optJSONArray("groups")?.let { a -> (0 until a.length()).mapNotNull { v.grup[a.optInt(it)] } } ?: emptyList()
    val tekrar = p.optJSONObject("repeat") ?: JSONObject()
    val icerik = p.optJSONArray("ingredients")?.let { a -> (0 until a.length()).map { a.optString(it) } } ?: emptyList()
    val secili = remember { mutableStateMapOf<String, List<Int>>().apply { mevcut?.secimler?.groupBy { it.slot }?.forEach { (s, l) -> put(s, l.map { it.id }) } } }
    var cikar by remember { mutableStateOf(mevcut?.cikar?.toSet() ?: emptySet()) }
    var not by remember { mutableStateOf(mevcut?.not ?: "") }
    var adet by remember { mutableIntStateOf(mevcut?.adet?.toInt()?.coerceAtLeast(1) ?: 1) }
    var uyari by remember { mutableStateOf<String?>(null) }

    fun secenek(g: JSONObject, id: Int): JSONObject? = g.optJSONArray("options").nesneler().firstOrNull { it.optInt("id") == id }
    fun dokun(slot: String, g: JSONObject, o: JSONObject) {
        val l = secili[slot].orEmpty(); val id = o.optInt("id"); val max = g.optInt("max")
        if (id in l) {
            secili[slot] = l - id
            secili.keys.filter { it.startsWith("$slot>$id>") }.forEach { secili.remove(it) }
        } else if (max == 1) {
            l.forEach { eski -> secili.keys.filter { it.startsWith("$slot>$eski>") }.forEach { secili.remove(it) } }
            secili[slot] = listOf(id)
        } else if (max > 0 && l.size >= max) uyari = "“${g.optString("name")}” için en fazla $max seçim yapılabilir."
        else secili[slot] = l + id
        if (uyari != null && !(max > 0 && l.size >= max && id !in l)) uyari = null
    }
    // Sıralı slot listesi: üst slot, ardından seçilen ürün-seçeneklerinin alt slotları
    data class Slot(val anahtar: String, val g: JSONObject, val etiket: String, val alt: Boolean)
    val slotlar = buildList<Slot> {
        gruplar.forEach { g ->
            val n = tekrar.optInt(g.optInt("id").toString(), 1).coerceAtLeast(1)
            for (k in 1..n) {
                val s = "${g.optInt("id")}:$k"
                add(Slot(s, g, (if (n > 1) "$k. " else "") + g.optString("name"), false))
                secili[s].orEmpty().forEach { oid ->
                    val o = secenek(g, oid)
                    val altPid = o?.optInt("product_id") ?: 0
                    if (altPid > 0) v.altGruplar(altPid).mapNotNull { v.grup[it] }.forEach { ag ->
                        add(Slot("$s>$oid>${ag.optInt("id")}", ag, "↳ " + o!!.optString("name") + " · " + ag.optString("name"), true))
                    }
                }
            }
        }
    }
    val ekFiyat = slotlar.sumOf { sl -> secili[sl.anahtar].orEmpty().sumOf { secenek(sl.g, it)?.optDouble("price") ?: 0.0 } }
    val taban = v.fiyat(p, tip)

    AlertDialog(
        onDismissRequest = kapat,
        title = {
            Column {
                Text(p.optString("name"), fontWeight = FontWeight.ExtraBold)
                Text(tl((taban + ekFiyat) * adet), color = Turuncu, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                slotlar.forEach { sl ->
                    val g = sl.g; val min = g.optInt("min"); val max = g.optInt("max"); val n = secili[sl.anahtar].orEmpty().size
                    Column(if (sl.alt) Modifier.padding(start = 12.dp) else Modifier) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(sl.etiket, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text(when { min > 0 && n < min -> "zorunlu"; max > 0 -> "$n/$max"; else -> "isteğe bağlı" }, fontSize = 11.sp, color = if (min > 0 && n < min) Kirmizi else Soluk)
                        }
                        Spacer(Modifier.height(4.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            g.optJSONArray("options").nesneler().forEach { o ->
                                val s = o.optInt("id") in secili[sl.anahtar].orEmpty()
                                FilterChip(selected = s, onClick = { dokun(sl.anahtar, g, o) },
                                    label = { Text((if (g.optBoolean("removal")) "Çıkar: " else "") + o.optString("name") + if (o.optDouble("price") > 0) " +" + tl(o.optDouble("price")) else "") },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TuruncuAcik, selectedLabelColor = Turuncu))
                            }
                        }
                    }
                }
                if (icerik.isNotEmpty()) Column {
                    Text("İçindekiler (çıkarmak için dokunun)", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        icerik.forEach { m ->
                            FilterChip(selected = m in cikar, onClick = { cikar = if (m in cikar) cikar - m else cikar + m },
                                label = { Text(if (m in cikar) "− $m" else m) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = KirmiziAcik, selectedLabelColor = Kirmizi))
                        }
                    }
                }
                OutlinedTextField(not, { not = it }, label = { Text("Not (mutfağa)") }, singleLine = true, colors = alanRenkleri(), modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                    OutlinedIconButton(onClick = { if (adet > 1) adet-- }) { Icon(Icons.Filled.Remove, "Azalt") }
                    Text(adet.toString(), fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, modifier = Modifier.padding(horizontal = 20.dp))
                    OutlinedIconButton(onClick = { adet++ }) { Icon(Icons.Filled.Add, "Artır") }
                }
                uyari?.let { Text(it, color = Kirmizi, fontSize = 13.sp) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val eksik = slotlar.firstOrNull { !it.alt && secili[it.anahtar].orEmpty().size < it.g.optInt("min") }
                if (eksik != null) uyari = "“${eksik.etiket}” seçimi zorunlu."
                else {
                val sec = slotlar.flatMap { sl -> secili[sl.anahtar].orEmpty().mapNotNull { id -> secenek(sl.g, id)?.let { o ->
                    Secim(id, (if (sl.alt) "↳ " else "") + (if (sl.g.optBoolean("removal")) "Çıkar: " else "") + o.optString("name"), o.optDouble("price"), sl.anahtar) } } }
                tamam(Kalem(urunId = pid, ad = p.optString("name"), adet = adet.toDouble(), taban = taban, secimler = sec, cikar = icerik.filter { it in cikar }, not = not.trim()))
                }
            }, colors = ButtonDefaults.buttonColors(containerColor = Turuncu)) { Text(if (mevcut == null) "Sepete ekle" else "Güncelle", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = kapat) { Text("Vazgeç", color = Soluk) } }
    )
}

/* ============================== ÖDEME ============================== */

@Composable
private fun OdemePenceresi(v: KasaVeri, ad: Adisyon, kapat: () -> Unit, bitti: (JSONObject) -> Unit) {
    val kapsam = rememberCoroutineScope()
    var ara by remember { mutableDoubleStateOf(ad.ara) }
    var kampanyalar by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var sadakat by remember { mutableStateOf<JSONObject?>(null) }
    var kampanya by remember { mutableIntStateOf(0) }
    var sadakatKullan by remember { mutableStateOf(false) }
    var kod by remember { mutableStateOf("") }
    var elle by remember { mutableStateOf("") }
    val odemeler = remember { mutableStateListOf<Pair<Int, Double>>() }
    var alinan by remember { mutableStateOf("") }
    var hata by remember { mutableStateOf<String?>(null) }
    var calisiyor by remember { mutableStateOf(false) }
    val ref = remember { UUID.randomUUID().toString() }

    suspend fun kampanyaHesapla() {
        runCatching {
            val r = Api.kasa("campaign_eval", JSONObject().put("type", ad.tip).put("phone", ad.tel).put("code", kod)
                .put("items", JSONArray(ad.kalemler.map { JSONObject().put("product_id", it.urunId).put("qty", it.adet).put("price", it.birim) })))
            kampanyalar = r.optJSONArray("campaigns").nesneler()
            sadakat = r.optJSONObject("loyalty")?.takeIf { it.optBoolean("reward") }
            if (kampanya == 0) kampanyalar.firstOrNull { it.optBoolean("auto") }?.let { kampanya = it.optInt("id") }
            if (kampanya != 0 && kampanyalar.none { it.optInt("id") == kampanya }) kampanya = 0
        }
    }
    LaunchedEffect(Unit) { kampanyaHesapla() }

    val kampInd = kampanyalar.firstOrNull { it.optInt("id") == kampanya }?.optDouble("discount") ?: 0.0
    val sadInd = if (sadakatKullan) sadakat?.optDouble("reward_amount") ?: 0.0 else 0.0
    val elleInd = elle.replace(',', '.').toDoubleOrNull() ?: 0.0
    val indirim = Math.round(minOf(ara, kampInd + sadInd + elleInd) * 100) / 100.0
    val toplam = Math.round((ara - indirim) * 100) / 100.0
    val odenen = Math.round(odemeler.sumOf { it.second } * 100) / 100.0
    val kalan = Math.round((toplam - odenen) * 100) / 100.0
    val nakitVar = odemeler.any { o -> v.yontemler.firstOrNull { it.optInt("id") == o.first }?.optString("kind") == "cash" }

    fun tamamla() {
        if (calisiyor) return
        calisiyor = true; hata = null
        kapsam.launch {
            try {
                val g = ad.govde {
                    put("discount", indirim)
                    put("campaign_id", kampanya)
                    put("loyalty_use", sadakatKullan)
                    put("client_ref", ref)
                    put("payments", JSONArray(odemeler.map { JSONObject().put("method_id", it.first).put("amount", it.second) }))
                }
                bitti(Api.kasa("checkout", g).optJSONObject("order") ?: JSONObject())
            } catch (e: ApiHata) { hata = e.message } finally { calisiyor = false }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!calisiyor) kapat() },
        title = { Text("Ödeme · " + TipAd[ad.tip], fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(Modifier.heightIn(max = 600.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                BilgiSatir("Ara toplam", tl(ara))
                if (kampanyalar.isNotEmpty()) Column {
                    Text("Kampanya", fontWeight = FontWeight.Bold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Cip2(kampanya == 0, "Yok") { kampanya = 0 }
                        kampanyalar.forEach { c -> Cip2(kampanya == c.optInt("id"), c.optString("name") + " −" + tl(c.optDouble("discount"))) { kampanya = c.optInt("id") } }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(kod, { kod = it.uppercase(TR) }, label = { Text("Kupon kodu") }, singleLine = true, colors = alanRenkleri(), modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = { kapsam.launch { kampanyaHesapla() } }) { Text("Uygula") }
                }
                sadakat?.let { s ->
                    Row(Modifier.fillMaxWidth().background(YesilAcik, RoundedCornerShape(12.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("🎁 Sadakat ödülü hazır", fontWeight = FontWeight.Bold, color = Yesil)
                            Text(s.optString("reward_text") + " · −" + tl(s.optDouble("reward_amount")), fontSize = 12.sp)
                        }
                        Switch(checked = sadakatKullan, onCheckedChange = { sadakatKullan = it }, colors = SwitchDefaults.colors(checkedTrackColor = Yesil))
                    }
                }
                OutlinedTextField(elle, { elle = it.filter { c -> c.isDigit() || c == ',' || c == '.' } }, label = { Text("Ek indirim (₺)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), colors = alanRenkleri(), modifier = Modifier.fillMaxWidth())
                if (indirim > 0) BilgiSatir("İndirim", "−" + tl(indirim), renk = Kirmizi)
                Row(Modifier.fillMaxWidth().background(Koyu, RoundedCornerShape(14.dp)).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Ödenecek", color = Color(0xFFD6D3D1), modifier = Modifier.weight(1f))
                    Text(tl(toplam), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp)
                }
                Text("Ödeme yöntemi " + if (odemeler.isEmpty()) "(dokununca kalan tutar eklenir)" else "", fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    v.yontemler.forEach { m ->
                        Button(onClick = { if (kalan > 0) odemeler.add(m.optInt("id") to kalan) }, enabled = kalan > 0,
                            colors = ButtonDefaults.buttonColors(containerColor = if (m.optString("kind") == "cash") Yesil else Mavi), shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(50.dp)) { Text(m.optString("name"), fontWeight = FontWeight.Bold) }
                    }
                }
                odemeler.forEachIndexed { i, (mid, tutar) ->
                    var duzen by remember(i, mid) { mutableStateOf(para(tutar).replace('.', ',')) }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(v.yontemler.firstOrNull { it.optInt("id") == mid }?.optString("name") ?: "?", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        OutlinedTextField(duzen, { y -> duzen = y.filter { c -> c.isDigit() || c == ',' || c == '.' }; y.replace(',', '.').toDoubleOrNull()?.let { odemeler[i] = mid to it } },
                            singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), colors = alanRenkleri(), modifier = Modifier.width(120.dp))
                        IconButton(onClick = { odemeler.removeAt(i) }) { Icon(Icons.Filled.Close, "Kaldır", tint = Soluk) }
                    }
                }
                if (kalan > 0.009 && odemeler.isNotEmpty()) Text("Kalan: " + tl(kalan), color = Sari, fontWeight = FontWeight.Bold)
                if (kalan < -0.009) Text("Fazla: " + tl(-kalan) + " — tutarları düzeltin", color = Kirmizi, fontWeight = FontWeight.Bold)
                if (nakitVar) {
                    val nakit = odemeler.filter { o -> v.yontemler.firstOrNull { it.optInt("id") == o.first }?.optString("kind") == "cash" }.sumOf { it.second }
                    Text("Para üstü hesabı", fontWeight = FontWeight.Bold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(50, 100, 200, 500, 1000).filter { it >= nakit }.take(4).forEach { b -> Cip2(alinan == b.toString(), "$b ₺") { alinan = b.toString() } }
                    }
                    OutlinedTextField(alinan, { alinan = it.filter { c -> c.isDigit() || c == ',' || c == '.' } }, label = { Text("Müşteriden alınan nakit") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), colors = alanRenkleri(), modifier = Modifier.fillMaxWidth())
                    alinan.replace(',', '.').toDoubleOrNull()?.takeIf { it >= nakit }?.let { Text("Para üstü: " + tl(it - nakit), fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Yesil) }
                }
                hata?.let { Text(it, color = Kirmizi, fontWeight = FontWeight.SemiBold) }
            }
        },
        confirmButton = {
            Button(onClick = { tamamla() }, enabled = odemeler.isNotEmpty() && kotlin.math.abs(kalan) < 0.01 && !calisiyor,
                colors = ButtonDefaults.buttonColors(containerColor = Yesil), modifier = Modifier.height(50.dp)) {
                if (calisiyor) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                else Text("Tahsil et · " + tl(toplam), fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = { TextButton(onClick = kapat, enabled = !calisiyor) { Text("Vazgeç", color = Soluk) } }
    )
}

/* ============================== MASALAR / PAKET / GELEN / BUGÜN ============================== */

@Composable
private fun MasalarGorunumu(durum: JSONObject, sec: (JSONObject) -> Unit) {
    val masalar = durum.optJSONArray("tables").nesneler()
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
        if (masalar.isEmpty()) item { BosKutu("Masa tanımlı değil.") }
        masalar.groupBy { it.optString("area").let { a -> if (a.isEmpty() || a == "null") "Salon" else a } }.forEach { (bolge, l) ->
            item { BolumBaslik(bolge + " · " + l.count { it.optInt("order_id") > 0 } + "/" + l.size + " dolu") }
            item {
                BoxWithConstraints {
                    val sutun = (maxWidth / 112.dp).toInt().coerceIn(3, 8)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), maxItemsInEachRow = sutun) {
                        l.forEach { t ->
                            val dolu = t.optInt("order_id") > 0
                            Column(
                                Modifier.weight(1f).height(86.dp).background(if (dolu) TuruncuAcik else Color.White, RoundedCornerShape(14.dp))
                                    .border(if (dolu) 2.dp else 1.dp, if (dolu) Turuncu else Cizgi, RoundedCornerShape(14.dp)).clickable { sec(t) }.padding(10.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(t.optString("name"), fontWeight = FontWeight.ExtraBold, maxLines = 1)
                                if (dolu) {
                                    Text(tl(t.optDouble("total")), fontWeight = FontWeight.Bold, color = Turuncu, fontSize = 13.sp)
                                    Text(t.optString("opened_at").let { if (it.length >= 16) it.substring(11, 16) else "" } + " · ${t.optInt("item_count")} ürün", color = Soluk, fontSize = 11.sp, maxLines = 1)
                                } else Text("boş · ${t.optInt("seats")} kişi", color = Soluk, fontSize = 11.sp)
                            }
                        }
                        repeat((sutun - l.size % sutun) % sutun) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

private val PaketDurum = listOf("preparing" to "Hazırlanıyor", "on_way" to "Yolda", "delivered" to "Teslim")

@Composable
private fun PaketGorunumu(durum: JSONObject, ac: (Int) -> Unit, durumDegis: (Int, String) -> Unit, yeni: (String) -> Unit) {
    val paket = durum.optJSONArray("deliveries").nesneler()
    val al = durum.optJSONArray("takeaways").nesneler()
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { yeni("delivery") }, colors = ButtonDefaults.buttonColors(containerColor = Turuncu), modifier = Modifier.weight(1f)) { Icon(Icons.Filled.DeliveryDining, null); Spacer(Modifier.width(6.dp)); Text("Yeni paket") }
                OutlinedButton(onClick = { yeni("takeaway") }, modifier = Modifier.weight(1f)) { Text("Yeni al götür", color = Murekkep) }
            }
        }
        item { BolumBaslik("Açık paketler · ${paket.size}") }
        if (paket.isEmpty()) item { BosKutu("Açık paket sipariş yok.") }
        items(paket, key = { "p" + it.optInt("id") }) { o ->
            Kart(onClick = { ac(o.optInt("id")) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("#" + o.optString("order_no") + " · " + o.optString("customer_name").let { if (it == "null") "" else it }, fontWeight = FontWeight.Bold)
                        Text(listOfNotNull(o.optString("channel_name").takeIf { it.isNotEmpty() && it != "null" }, o.optString("courier_name").takeIf { it.isNotEmpty() && it != "null" }?.let { "🛵 $it" },
                            o.optString("created_at").let { if (it.length >= 16) it.substring(11, 16) else null }).joinToString(" · "), color = Soluk, fontSize = 12.sp)
                        o.optString("customer_address").takeIf { it.isNotEmpty() && it != "null" }?.let { Text(it, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis) }
                    }
                    Text(tl(o.optDouble("total")), fontWeight = FontWeight.ExtraBold)
                }
                Spacer(Modifier.height(6.dp))
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    PaketDurum.forEachIndexed { i, (k, ad) ->
                        SegmentedButton(selected = o.optString("delivery_status") == k, onClick = { durumDegis(o.optInt("id"), k) }, shape = SegmentedButtonDefaults.itemShape(i, 3),
                            colors = SegmentedButtonDefaults.colors(activeContainerColor = if (k == "on_way") MaviAcik else if (k == "delivered") YesilAcik else SariAcik)) { Text(ad, fontSize = 12.sp, maxLines = 1) }
                    }
                }
            }
        }
        item { BolumBaslik("Açık al götür · ${al.size}") }
        items(al, key = { "a" + it.optInt("id") }) { o ->
            Kart(onClick = { ac(o.optInt("id")) }) {
                Row {
                    Text("#" + o.optString("order_no") + " " + o.optString("customer_name").let { if (it == "null") "" else it }, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text(tl(o.optDouble("total")), fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun GelenGorunumu(gelen: List<JSONObject>, kabul: (Int) -> Unit, red: (JSONObject) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
        if (gelen.isEmpty()) item { BosKutu("Bekleyen online sipariş yok. Yeni sipariş gelince burada ve üstteki zil simgesinde görünür.") }
        items(gelen, key = { it.optInt("id") }) { o ->
            Kart {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(o.optString("platform_name") + " · " + o.optString("customer_name"), fontWeight = FontWeight.ExtraBold)
                        Text(listOfNotNull(o.optString("table_name").takeIf { it.isNotEmpty() && it != "null" }?.let { "Masa $it" }, o.optString("phone").takeIf { it.isNotEmpty() && it != "null" },
                            if (o.optBoolean("paid_online")) "online ödendi" else o.optString("payment").ifEmpty { null }, tarihSaat(o.optString("created_at"))).joinToString(" · "), color = Soluk, fontSize = 12.sp)
                    }
                    Text(tl(o.optDouble("total")), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                }
                o.optString("address").takeIf { it.isNotEmpty() && it != "null" }?.let { Text("📍 $it", fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp)) }
                Spacer(Modifier.height(6.dp))
                o.optJSONArray("items").nesneler().forEach { i ->
                    Text("${i.optDouble("qty").let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() }} × " + i.optString("name") + (if (!i.optBoolean("matched")) " ⚠" else "") +
                        (if (i.optString("note").isNotEmpty() && i.optString("note") != "null") " · " + i.optString("note") else ""), fontSize = 14.sp)
                }
                o.optString("note").takeIf { it.isNotEmpty() && it != "null" }?.let { Text("📝 $it", color = Sari, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp)) }
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { red(o) }, modifier = Modifier.weight(1f)) { Text("Reddet", color = Kirmizi) }
                    Button(onClick = { kabul(o.optInt("id")) }, colors = ButtonDefaults.buttonColors(containerColor = Yesil), modifier = Modifier.weight(2f)) { Text("Kabul et", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable
private fun BugunGorunumu(ac: (Int) -> Unit, yazdir: (Int) -> Unit) {
    var veri by remember { mutableStateOf<JSONObject?>(null) }
    LaunchedEffect(Unit) { veri = runCatching { Api.kasa("history") }.getOrNull() }
    val v = veri ?: run { Yukleniyor(); return }
    val bugun = v.optJSONObject("today") ?: JSONObject()
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
        item {
            Row(Modifier.fillMaxWidth().background(Koyu, RoundedCornerShape(16.dp)).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Bugün (iş günü)", color = Color(0xFFD6D3D1), fontSize = 12.sp)
                    Text(tl(bugun.optDouble("total")), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp)
                }
                Text("${bugun.optInt("count")} satış", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        items(v.optJSONArray("orders").nesneler(), key = { it.optInt("id") }) { o ->
            val iptal = o.optString("status") == "cancelled"
            Row(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(12.dp)).clickable { ac(o.optInt("id")) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("#" + o.optString("order_no") + " · " + (TipAd[o.optString("type")] ?: "") + (o.optString("table_name").takeIf { it.isNotEmpty() && it != "null" }?.let { " $it" } ?: ""),
                        fontWeight = FontWeight.Bold, color = if (iptal) Soluk else Murekkep)
                    Text(o.optString("closed_at").let { if (it.length >= 16) it.substring(11, 16) else "" } + " · " + (if (iptal) "İPTAL" else o.optString("methods").let { if (it == "null") "" else it }),
                        color = if (iptal) Kirmizi else Soluk, fontSize = 12.sp)
                }
                Text(tl(o.optDouble("total")), fontWeight = FontWeight.ExtraBold, color = if (iptal) Soluk else Murekkep)
                if (!iptal) IconButton(onClick = { yazdir(o.optInt("id")) }) { Icon(Icons.Filled.Print, "Fiş", tint = Soluk) }
            }
        }
    }
}

/* ============================== PENCERELER ============================== */

@Composable
private fun NedenPenceresi(baslik: String, metin: String, kapat: () -> Unit, tamam: (String) -> Unit) {
    var n by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = kapat, title = { Text(baslik, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(metin)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Müşteri vazgeçti", "Yanlış giriş", "Ürün kalmadı", "Yoğunluk").forEach { Cip2(n == it, it) { n = it } }
                }
                OutlinedTextField(n, { n = it }, label = { Text("Neden") }, singleLine = true, colors = alanRenkleri(), modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { Button(onClick = { tamam(n.trim()) }, colors = ButtonDefaults.buttonColors(containerColor = Kirmizi)) { Text("Onayla", fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = kapat) { Text("Vazgeç", color = Soluk) } })
}

@Composable
private fun MasaSecPenceresi(durum: JSONObject, mevcut: Int, kapat: () -> Unit, sec: (Int) -> Unit) {
    AlertDialog(onDismissRequest = kapat, title = { Text("Hangi masaya?", fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState())) {
                Text("Dolu masa seçerseniz iki adisyon birleştirilir.", color = Soluk, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    durum.optJSONArray("tables").nesneler().filter { it.optInt("id") != mevcut }.forEach { t ->
                        Cip2(false, t.optString("name") + if (t.optInt("order_id") > 0) " · dolu" else "") { sec(t.optInt("id")) }
                    }
                }
            }
        },
        confirmButton = {}, dismissButton = { TextButton(onClick = kapat) { Text("Vazgeç", color = Soluk) } })
}

@Composable
private fun GiderPenceresi(v: KasaVeri, kapat: () -> Unit, kaydet: (JSONObject) -> Unit) {
    FormPenceresi("Kasadan gider öde", listOf(
        Alan("category_id", "Kategori", AlanTip.SECIM, secenekler = v.giderKat.map { it.optInt("id").toString() to it.optString("name") }),
        Alan("amount", "Tutar (₺)", AlanTip.SAYI),
        Alan("description", "Ne için?"),
        Alan("supplier", "Kime ödendi? (isteğe bağlı)"),
    ), dugme = "Kasadan öde", kapat = kapat) { m -> kaydet(JSONObject().apply { m.forEach { (k, d) -> put(k, d) } }) }
}

@Composable
private fun KasaKapatPenceresi(s: JSONObject, kapat: () -> Unit, kaydet: (JSONObject) -> Unit) {
    var sayilan by remember { mutableStateOf("") }
    var not by remember { mutableStateOf("") }
    val beklenen = s.optDouble("expected")
    val fark = sayilan.replace(',', '.').toDoubleOrNull()?.let { it - beklenen }
    AlertDialog(onDismissRequest = kapat, title = { Text("Kasa kapanışı", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                BilgiSatir("Açılış (" + tarihSaat(s.optString("opened_at")) + ")", tl(s.optDouble("opening")))
                BilgiSatir("Nakit satış", "+" + tl(s.optDouble("cash_sales")))
                BilgiSatir("Kasadan gider", "−" + tl(s.optDouble("cash_expenses")))
                BilgiSatir("Kart / diğer (bilgi)", tl(s.optDouble("other_sales")), alt = "${s.optInt("orders")} satış")
                BilgiSatir("Çekmecede olması gereken", tl(beklenen), kalin = true)
                OutlinedTextField(sayilan, { sayilan = it.filter { c -> c.isDigit() || c == ',' || c == '.' } }, label = { Text("Sayılan nakit (₺)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), colors = alanRenkleri(), modifier = Modifier.fillMaxWidth())
                fark?.let { Text(if (kotlin.math.abs(it) < 0.01) "Kasa tutuyor ✓" else (if (it < 0) "Açık: " else "Fazla: ") + tl(kotlin.math.abs(it)),
                    fontWeight = FontWeight.ExtraBold, color = if (kotlin.math.abs(it) < 0.01) Yesil else if (it < 0) Kirmizi else Sari) }
                OutlinedTextField(not, { not = it }, label = { Text("Not") }, singleLine = true, colors = alanRenkleri(), modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { Button(onClick = { kaydet(JSONObject().put("counted", sayilan.replace(',', '.').toDoubleOrNull() ?: 0.0).put("note", not)) }, enabled = sayilan.isNotEmpty(),
            colors = ButtonDefaults.buttonColors(containerColor = Turuncu)) { Text("Kasayı kapat", fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = kapat) { Text("Vazgeç", color = Soluk) } })
}

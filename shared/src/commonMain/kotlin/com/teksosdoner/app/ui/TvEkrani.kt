@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.teksosdoner.app.ui

import com.teksosdoner.app.*

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teksosdoner.app.Api
import com.teksosdoner.app.ApiHata
import com.teksosdoner.app.nesneler
import com.teksosdoner.app.tl
import kotlinx.coroutines.launch
import com.teksosdoner.app.json.JSONArray
import com.teksosdoner.app.json.JSONObject

private fun JSONArray?.sayilar(): List<Int> = if (this == null) emptyList() else (0 until length()).map { optInt(it) }

/* ============================== TV EKRANLARI ============================== */

@Composable
fun TvEkrani(geri: (() -> Unit)?, panoAc: (Int) -> Unit, onizle: (String, String) -> Unit) {
    VeriEkrani("TV Ekranları", "y_tv", geri, altBaslik = { "${it.optJSONArray("ekranlar")?.length() ?: 0} ekran · bu şubeye özel" },
        fab = "Ekran ekle" to { _, k -> k.islem("y_tv_islem", govde("ekle")) }
    ) { v, k ->
        val urunler = v.optJSONArray("urunler").nesneler()
        val kat = v.optJSONArray("kategoriler").nesneler()
        val katAd = kat.associate { it.optInt("id") to it.optString("ad") }
        if (v.optInt("foto_sayi") < urunler.size / 2) item {
            Serit(Icons.Filled.Warning, "TV’lerin güzel görünmesi ürün fotoğraflarına bağlı: ${v.optInt("foto_sayi")}/${urunler.size} ürünün fotoğrafı var. Fotoğrafı olmayanlar baş harfli rozetle gösterilir.", Sari, SariAcik)
        }
        item {
            Kart {
                KartBaslik(Icons.Filled.Tv, "TV’ye kurulum", renk = Mavi)
                Text("Her TV’nin tarayıcısında bu adresi açıp TV numarasını seçin; seçim o cihazda hatırlanır.", color = Soluk, fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                Text(v.optString("adres"), fontWeight = FontWeight.Bold, color = Mavi, fontSize = 13.sp)
                EylemSatiri(
                    Triple("Kopyala", Mavi) { os.kopyala(v.optString("adres")) },
                    Triple("Paylaş", Mavi) { os.paylas(v.optString("adres")) },
                )
            }
        }
        val g = v.optJSONObject("genel") ?: JSONObject()
        item {
            Kart(onClick = {
                k.ac {
                    FormPenceresi("Görünüm (tüm TV’ler)", listOf(
                        Alan("tema", "Tema", AlanTip.SECIM, g.optString("tema"), v.optJSONArray("temalar").nesneler().map { it.optString("kod") to it.optString("ad") }),
                        Alan("baslik", "İşletme adı (logo yoksa yazılır)", deger = g.optString("baslik")),
                        Alan("fiyat", "Gösterilecek fiyat", AlanTip.SECIM, g.optString("fiyat"), listOf("takeaway" to "Al götür", "table" to "Masa", "delivery" to "Paket")),
                        Alan("alt", "Alt kayan yazı", AlanTip.UZUN, g.optString("alt")),
                        Alan("bant", "Alt bantta kampanyalar ve kayan yazı", AlanTip.ANAHTAR, if (g.optBoolean("bant")) "1" else ""),
                        Alan("saat", "Saat", AlanTip.ANAHTAR, if (g.optBoolean("saat")) "1" else ""),
                        Alan("cok_satan", "“🔥 Çok satan” etiketi (son 30 gün ilk 3)", AlanTip.ANAHTAR, if (g.optBoolean("cok_satan")) "1" else ""),
                    ) + if (!g.isNull("logo")) listOf(Alan("logo_sil", "Logoyu kaldır", AlanTip.ANAHTAR)) else emptyList(), kapat = k.kapat) { k.islem("y_tv_islem", govde("genel", null, it)) }
                }
            }) {
                KartBaslik(Icons.Filled.Edit, "Görünüm", "düzenle")
                BilgiSatir("Tema", v.optJSONArray("temalar").nesneler().firstOrNull { it.optString("kod") == g.optString("tema") }?.optString("ad") ?: g.optString("tema"))
                BilgiSatir("Fiyat", mapOf("takeaway" to "Al götür", "table" to "Masa", "delivery" to "Paket")[g.optString("fiyat")] ?: "")
                if (g.optString("alt").isNotEmpty()) BilgiSatir("Kayan yazı", g.optString("alt"))
                if (!g.isNull("logo")) UzakResim(g.optString("logo"), Modifier.height(48.dp).background(Koyu, RoundedCornerShape(8.dp)).padding(6.dp))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { k.ac { OnayPenceresi("Teksos menü panoları", "Teksos menü görselleri 4 TV’ye kurulsun mu? Mevcut ekran ayarları değişir.", "Kur", tehlikeli = false, kapat = k.kapat) { k.islem("y_tv_islem", govde("pano4")) } } }, modifier = Modifier.weight(1f)) {
                    Text("🖼 4 Teksos panosu", color = Murekkep, maxLines = 1)
                }
                OutlinedButton(onClick = { k.ac { OnayPenceresi("Hazır 4’lü plan", "Mevcut ekran ayarları silinip kategorilere göre otomatik 4 ekran kurulsun mu?", "Uygula", kapat = k.kapat) { k.islem("y_tv_islem", govde("varsayilan")) } } }, modifier = Modifier.weight(1f)) {
                    Text("Hazır 4’lü plan", color = Murekkep, maxLines = 1)
                }
            }
        }
        val atanmamis = kat.filter { !it.optBoolean("atanmis") && it.optInt("urun") > 0 }
        if (atanmamis.isNotEmpty()) item { Serit(Icons.Filled.Warning, "Hiçbir TV’de gösterilmeyen kategori: " + atanmamis.joinToString(", ") { it.optString("ad") }, Mavi, MaviAcik) }
        val ekranlar = v.optJSONArray("ekranlar").nesneler()
        items(ekranlar, key = { "e" + it.optInt("no") }) { e ->
            val no = e.optInt("no")
            Kart {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(44.dp).background(Koyu, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) { Text("TV\n${no + 1}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.Center, lineHeight = 12.sp) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(e.optString("ad"), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(e.optString("duzen_ad").substringBefore(" (") + " · " + (if (e.optString("yon") == "dikey") "dikey" else "yatay") + " · ${e.optInt("sure")} sn", color = Soluk, fontSize = 12.sp)
                    }
                }
                val katlar = e.optJSONArray("kategoriler").sayilar().mapNotNull { katAd[it] }
                if (e.optString("duzen") == "pano") Text(e.optString("pano_ad").substringBefore(" (").ifEmpty { "Teksos menü görseli" } + " · ${e.optInt("pano_parca")} eklenmiş parça", fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
                else if (katlar.isNotEmpty()) Text("Kategoriler: " + katlar.joinToString(", "), fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
                val ur = e.optJSONArray("urunler").sayilar()
                if (ur.isNotEmpty()) Text("Vitrin: ${ur.size} ürün", fontSize = 13.sp, color = Soluk)
                EylemSatiri(
                    *listOfNotNull<Triple<String, Color, () -> Unit>>(
                        Triple("Önizle", Mavi) { onizle(e.optString("adres"), "TV ${no + 1} · " + e.optString("ad")) },
                        if (e.optString("duzen") == "pano") Triple("Panoyu düzenle", Turuncu) { panoAc(no) } else null,
                        Triple("Ayarlar", Mavi) {
                            k.ac {
                                FormPenceresi("TV ${no + 1}", listOf(
                                    Alan("ad", "Ekran adı", deger = e.optString("ad")),
                                    Alan("baslik", "Üstteki başlık (boş = kategori adı)", deger = e.optString("baslik")),
                                    Alan("duzen", "Düzen", AlanTip.SECIM, e.optString("duzen"), v.optJSONArray("duzenler").nesneler().map { it.optString("kod") to it.optString("ad") }),
                                    Alan("yon", "TV yönü", AlanTip.SECIM, e.optString("yon"), listOf("yatay" to "Yatay (normal)", "dikey" to "Dikey (duvara dik asılı)")),
                                    Alan("sure", "Sayfa / slayt süresi (sn)", AlanTip.SAYI, e.optInt("sure").toString()),
                                    Alan("kategoriler", "Kategoriler (kart / liste / karma)", AlanTip.COKLU, e.optJSONArray("kategoriler").sayilar().joinToString(","), kat.map { it.optInt("id").toString() to it.optString("ad") }),
                                    Alan("urunler", "Vitrinde dönecek ürünler (boş = çok satanlar)", AlanTip.COKLU, e.optJSONArray("urunler").sayilar().joinToString(","),
                                        urunler.map { it.optInt("id").toString() to (it.optString("ad") + if (it.optBoolean("foto")) " 📷" else "") }),
                                ), kapat = k.kapat) { k.islem("y_tv_islem", govde("ekran", null, it + ("no" to no.toString()))) }
                            }
                        },
                        if (ekranlar.size > 1) Triple("Sil", Kirmizi) { k.ac { OnayPenceresi("TV ${no + 1} silinsin mi?", e.optString("ad"), "Sil", kapat = k.kapat) { k.islem("y_tv_islem", JSONObject().put("act", "sil").put("no", no)) } } } else null,
                    ).toTypedArray()
                )
            }
        }
    }
}

/* ============================== PANO DÜZENLEYİCİ ============================== */

private val TipAdi = mapOf("etiket" to "Etiket", "fiyat" to "Fiyat kutusu", "tukendi" to "Tükendi alanı", "not" to "Yazı", "baslik" to "Başlık", "resim" to "Görsel",
    "panel" to "Kutu (üstünü kapat)", "tablo" to "Fiyat tablosu", "kart" to "Menü kartı", "combo" to "Kişilik menü kartı", "vitrin" to "Öne çıkan ürün",
    "daire" to "Sandviç (yuvarlak fiyat)", "icecek" to "İçecek listesi", "ekstra" to "Ekstralar listesi")

private val Sablonlar: Map<String, String> = mapOf(
    "etiket" to """{"tip":"etiket","x":820,"y":460,"w":280,"metin":"YENİ!","renk":"kirmizi","yazi":"beyaz","boyut":56,"sekil":"kutu","aci":-8}""",
    "fiyat" to """{"tip":"fiyat","x":893,"y":505,"w":135,"h":69,"urun":null,"fiyat":""}""",
    "fiyat_dr" to """{"tip":"fiyat","x":920,"y":500,"w":81,"h":81,"sekil":"daire","urun":null,"fiyat":""}""",
    "not" to """{"tip":"not","x":760,"y":500,"w":400,"metin":"Yazınız","renk":"siyah","boyut":44}""",
    "baslik" to """{"tip":"baslik","x":660,"y":470,"w":600,"metin":"YENİ BAŞLIK","renk":"turuncu","boyut":80}""",
    "resim" to """{"tip":"resim","x":810,"y":390,"w":300,"resim":""}""",
    "panel" to """{"tip":"panel","x":860,"y":490,"w":200,"h":100,"renk":"gri","kose":12}""",
    "tukendi" to """{"tip":"tukendi","x":760,"y":400,"w":400,"h":280,"urun":null,"fiyat":""}""",
    "kart" to """{"tip":"kart","x":870,"y":430,"w":182,"h":220,"baslik":"MENÜ","aciklama":"","urun":null,"fiyat":"","resim":"","renk":"#E02424"}""",
)
private val EkleAd = listOf("etiket" to "🏷 Etiket", "fiyat" to "💲 Fiyat kutusu", "fiyat_dr" to "⭕ Yuvarlak fiyat", "not" to "✏️ Yazı", "baslik" to "🔤 Başlık",
    "resim" to "🖼 Görsel", "panel" to "⬜ Kutu", "tukendi" to "⛔ Tükendi alanı", "kart" to "Menü kartı")

private fun JSONObject.kopya(): JSONObject = JSONObject(toString())
private fun JSONObject.yukseklik(): Int = when {
    has("h") && optInt("h") > 0 -> optInt("h")
    optString("tip") in listOf("etiket", "not", "baslik") -> (optInt("boyut", 50) * 1.35).toInt()
    optString("tip") == "resim" -> (optInt("w") * 0.75).toInt()
    has("satirlar") -> ((optJSONArray("satirlar")?.length() ?: 1) * optInt("satir", 56) + optInt("boyut", 50)).coerceAtLeast(80)
    else -> (optInt("w") * 0.6).toInt()
}

@Composable
fun TvPanoEkrani(no: Int, geri: () -> Unit, onizle: (String, String) -> Unit) {
    val kapsam = rememberCoroutineScope()
    var veri by remember { mutableStateOf<JSONObject?>(null) }
    var hata by remember { mutableStateOf<String?>(null) }
    var mesaj by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    val bloklar = remember { mutableStateListOf<JSONObject>() }
    var secili by remember { mutableIntStateOf(-1) }
    var arka by remember { mutableStateOf("") }
    var kirli by remember { mutableStateOf(false) }
    var pencere by remember { mutableStateOf<(@Composable () -> Unit)?>(null) }
    var ekleMenu by remember { mutableStateOf(false) }
    val kapat = { pencere = null }

    suspend fun yukle() {
        try {
            val d = Api.get("y_tv_pano", mapOf("no" to no.toString()))
            veri = d; hata = null; arka = d.optString("arka")
            bloklar.clear(); d.optJSONArray("bloklar").nesneler().forEachIndexed { i, b -> bloklar.add(b.kopya().put("i", i)) }
            secili = -1; kirli = false
        } catch (e: ApiHata) { hata = e.message }
    }
    fun guncelle(i: Int, f: JSONObject.() -> Unit) { if (i in bloklar.indices) { bloklar[i] = bloklar[i].kopya().apply(f); kirli = true } }
    fun kaydet() {
        kapsam.launch {
            try {
                val liste = JSONArray(bloklar.map { b ->
                    JSONObject(b.toString()).apply { remove("resim_url"); remove("sutunlar"); if (optString("tip") == "tablo") remove("satirlar") }
                })
                mesaj = Api.post("y_tv_pano_kaydet", JSONObject().put("no", no).put("bloklar", liste).put("arka", arka)).optString("mesaj") to false
                yukle()
            } catch (e: ApiHata) { mesaj = (e.message ?: "Hata") to true }
        }
    }
    LaunchedEffect(no) { yukle() }

    val v = veri
    val urunler = remember(v) { v?.optJSONArray("urunler").nesneler() }
    val urunAd = remember(urunler) { urunler.orEmpty().associate { it.optInt("id") to it } }
    val renkler = remember(v) { v?.optJSONArray("renkler").nesneler().orEmpty().associate { it.optString("kod") to it.optString("hex") } }
    val gorselUrl = remember(v) { (v?.optJSONArray("gorseller").nesneler().orEmpty() + v?.optJSONArray("arkalar").nesneler().orEmpty()).associate { it.optString("kod") to it.optString("url") } }
    fun renkAl(r: String?, yedek: Color) = renkCoz(renkler[r] ?: r, yedek)

    Ekran("TV ${no + 1} panosu" + if (kirli) " •" else "", altBaslik = v?.optString("ad"), geri = geri, eylemler = {
        if (v != null) IconButton(onClick = { onizle(v.optString("adres"), "TV ${no + 1}") }) { Icon(Icons.Filled.Visibility, "TV’de nasıl görünüyor") }
        IconButton(onClick = { kapsam.launch { yukle() } }) { Icon(Icons.Filled.Refresh, "Değişiklikleri at") }
        IconButton(onClick = { kaydet() }, enabled = kirli) { Icon(Icons.Filled.Save, "Kaydet", tint = if (kirli) Turuncu else Soluk) }
    }) {
        when {
            v == null && hata != null -> HataKutusu(hata!!) { kapsam.launch { yukle() } }
            v == null -> Yukleniyor()
            else -> Column(Modifier.fillMaxSize()) {
                MesajSeridi(mesaj?.first, mesaj?.second ?: false) { mesaj = null }
                // ---- Tuval (1920×1080) ----
                BoxWithConstraints(Modifier.fillMaxWidth().padding(8.dp).aspectRatio(16f / 9f).background(Color(0xFF2A2522), RoundedCornerShape(8.dp))) {
                    val olcek = with(LocalDensity.current) { maxWidth.toPx() } / 1920f
                    val dp1 = with(LocalDensity.current) { 1f.toDp() }
                    (gorselUrl[arka] ?: if (arka.isNotEmpty() && arka == v.optString("arka")) v.optString("arka_url").takeIf { it != "null" } else null)?.let { UzakResim(it, Modifier.fillMaxSize(), ContentScale.FillBounds) }
                    Box(Modifier.fillMaxSize().clickable { secili = -1 })
                    bloklar.forEachIndexed { i, b ->
                        val sec = i == secili
                        val tip = b.optString("tip")
                        val w = b.optInt("w", 300); val h = b.yukseklik()
                        val zemin = when (tip) {
                            "etiket" -> renkAl(b.optString("renk"), Kirmizi)
                            "panel" -> renkAl(b.optString("renk"), Color(0xFFEFEFEF))
                            "fiyat" -> Color(0xFFFFFFFF).copy(alpha = 0.85f)
                            "tukendi" -> Color(0x99000000)
                            "kart", "combo", "vitrin" -> renkAl(b.optString("renk"), Kirmizi).copy(alpha = 0.85f)
                            "resim" -> Color.Transparent
                            else -> Color(0x33FFFFFF)
                        }
                        Box(
                            Modifier.offset(x = dp1 * (b.optInt("x") * olcek), y = dp1 * (b.optInt("y") * olcek))
                                .size(width = dp1 * (w * olcek), height = dp1 * (h * olcek))
                                .rotate(b.optInt("aci").toFloat())
                                .background(zemin, if (b.optString("sekil") == "daire") CircleShape else RoundedCornerShape((b.optInt("kose", 4) * olcek).coerceAtMost(40f).toInt().dp))
                                .border(if (sec) 2.dp else 1.dp, if (sec) Color(0xFF2563EB) else Color.White.copy(alpha = 0.5f))
                                .pointerInput(i) {
                                    detectDragGestures(onDragStart = { secili = i }) { ch, d ->
                                        ch.consume()
                                        guncelle(i) { put("x", (optInt("x") + d.x / olcek).toInt().coerceIn(-400, 2400)); put("y", (optInt("y") + d.y / olcek).toInt().coerceIn(-400, 2400)) }
                                    }
                                }
                                .clickable { secili = i },
                            contentAlignment = Alignment.Center
                        ) {
                            if (tip == "resim" || (tip == "kart" && b.optString("resim").isNotEmpty())) {
                                (b.optString("resim_url").takeIf { it.isNotEmpty() && it != "null" } ?: gorselUrl[b.optString("resim")])?.let { UzakResim(it, Modifier.fillMaxSize()) }
                            }
                            val etiket = when (tip) {
                                "fiyat", "tukendi" -> urunAd[b.optInt("urun")]?.let { u -> if (tip == "tukendi") "⛔ " + u.optString("ad") else tl(u.optDouble("fiyat")) } ?: b.optString("fiyat").ifEmpty { "₺" }
                                "kart", "combo", "vitrin" -> b.optString("baslik")
                                "panel", "resim" -> ""
                                "tablo" -> "Fiyat tablosu"
                                else -> b.optString("metin").ifEmpty { TipAdi[tip] ?: tip }
                            }
                            if (etiket.isNotEmpty()) Text(etiket, color = if (tip == "fiyat") Color.Black else if (tip == "not") renkAl(b.optString("renk"), Color.Black) else Color.White,
                                fontSize = (b.optInt("boyut", 40) * olcek * 0.75f).coerceIn(6f, 22f).sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Clip, textAlign = TextAlign.Center, lineHeight = 10.sp)
                        }
                        if (sec) Box(
                            Modifier.offset(x = dp1 * ((b.optInt("x") + w) * olcek) - 11.dp, y = dp1 * ((b.optInt("y") + h) * olcek) - 11.dp).size(22.dp)
                                .background(Color(0xFF2563EB), CircleShape).border(2.dp, Color.White, CircleShape)
                                .pointerInput("b$i") {
                                    detectDragGestures { ch, d ->
                                        ch.consume()
                                        guncelle(i) {
                                            put("w", (optInt("w", 300) + d.x / olcek).toInt().coerceIn(30, 2000))
                                            if (has("h") || optString("tip") in listOf("panel", "tukendi", "kart", "fiyat")) put("h", (yukseklik() + d.y / olcek).toInt().coerceIn(20, 1200))
                                            else if (optString("tip") in listOf("etiket", "not", "baslik")) put("boyut", (optInt("boyut", 50) + d.y / olcek / 1.35f).toInt().coerceIn(10, 240))
                                            if (optString("sekil") == "daire") put("h", optInt("w"))
                                        }
                                    }
                                }
                        )
                    }
                }
                // ---- Araç çubuğu ----
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box {
                        Button(onClick = { ekleMenu = true }, colors = ButtonDefaults.buttonColors(containerColor = Turuncu)) { Icon(Icons.Filled.Add, null); Text("Parça ekle") }
                        DropdownMenu(expanded = ekleMenu, onDismissRequest = { ekleMenu = false }) {
                            EkleAd.forEach { (kod, ad) ->
                                DropdownMenuItem(text = { Text(ad) }, onClick = {
                                    ekleMenu = false
                                    val kay = (bloklar.size % 6) * 70 - 175
                                    val b = JSONObject(Sablonlar[kod]!!).put("yeni", kod)
                                    b.put("x", b.optInt("x") + kay); b.put("y", b.optInt("y") + (kay * 0.6).toInt())
                                    bloklar.add(b); secili = bloklar.size - 1; kirli = true
                                })
                            }
                        }
                    }
                    OutlinedButton(onClick = {
                        pencere = {
                            AlertDialog(onDismissRequest = kapat, title = { Text("Arka plan / şablon", fontWeight = FontWeight.Bold) }, text = {
                                Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Arka plan görseli", fontWeight = FontWeight.Bold)
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Cip2(arka.isEmpty(), "Yok") { arka = ""; kirli = true; kapat() }
                                        v.optJSONArray("arkalar").nesneler().forEach { a -> Cip2(arka == a.optString("kod"), a.optString("ad").substringBefore('.')) { arka = a.optString("kod"); kirli = true; kapat() } }
                                    }
                                    Ayrac()
                                    Text("Hazır şablon uygula (eklenen parçalar silinir)", fontWeight = FontWeight.Bold)
                                    listOf("orijinal_1" to "Orijinal 1 — Dürüm Menü", "orijinal_2" to "Orijinal 2 — Burgerler", "orijinal_3" to "Orijinal 3 — Tako + İçecek", "orijinal_4" to "Orijinal 4 — Dürüm + Porsiyon",
                                        "durum_menu" to "Dürüm Menü (canlı fiyat)", "burger" to "Burgerler (canlı fiyat)", "tako_icecek" to "Tako + İçecekler (canlı fiyat)", "durum_porsiyon" to "Dürüm + Porsiyon (canlı fiyat)").forEach { (k, ad) ->
                                        Row(Modifier.fillMaxWidth().background(Zemin, RoundedCornerShape(10.dp)).clickable {
                                            kapat(); kapsam.launch {
                                                try { mesaj = Api.post("y_tv_islem", JSONObject().put("act", "sablon").put("no", no).put("sablon", k)).optString("mesaj") to false; yukle() } catch (e: ApiHata) { mesaj = (e.message ?: "") to true }
                                            }
                                        }.padding(12.dp)) { Text(ad) }
                                    }
                                }
                            }, confirmButton = {}, dismissButton = { TextButton(onClick = kapat) { Text("Kapat") } })
                        }
                    }) { Icon(Icons.Filled.Image, null, tint = Murekkep); Text(" Arka plan", color = Murekkep) }
                    Spacer(Modifier.weight(1f))
                    if (kirli) Text("kaydedilmedi", color = Sari, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                // ---- Seçili parçanın ayarları / parça listesi ----
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val b = bloklar.getOrNull(secili)
                    if (b == null) {
                        item { Text("Parçayı tuvalde parmağınızla sürükleyin, seçince mavi tutamaçtan büyütün. Ayarları için aşağıdan seçin.", color = Soluk, fontSize = 13.sp) }
                        if (bloklar.isEmpty()) item { BosKutu("Bu panoda eklenmiş parça yok. “Parça ekle” ile etiket, fiyat, yazı ekleyin.") }
                        items(bloklar.size) { i ->
                            val x = bloklar[i]
                            Row(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(12.dp)).clickable { secili = i }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(TipAdi[x.optString("tip")] ?: x.optString("tip"), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                Text(x.optString("metin").ifEmpty { x.optString("baslik").ifEmpty { urunAd[x.optInt("urun")]?.optString("ad") ?: "" } }, color = Soluk, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 180.dp))
                            }
                        }
                    } else {
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(TipAdi[b.optString("tip")] ?: b.optString("tip"), fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, modifier = Modifier.weight(1f))
                                IconButton(onClick = { bloklar.add(b.kopya().apply { put("y", optInt("y") + if (optString("tip") == "fiyat") (yukseklik() * 1.167).toInt() else yukseklik() + 12) }); secili = bloklar.size - 1; kirli = true }) {
                                    Icon(Icons.Filled.ContentCopy, "Altına kopyala")
                                }
                                IconButton(onClick = { bloklar.removeAt(secili); secili = -1; kirli = true }) { Icon(Icons.Filled.Delete, "Sil", tint = Kirmizi) }
                                TextButton(onClick = { secili = -1 }) { Text("Bitti") }
                            }
                        }
                        item { PanoAyarlari(b, urunler.orEmpty(), v.optJSONArray("renkler").nesneler(), v.optJSONArray("gorseller").nesneler(), urunAd) { f -> guncelle(secili, f) } }
                    }
                    item { Spacer(Modifier.height(4.dp)); Button(onClick = { kaydet() }, enabled = kirli, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = Turuncu)) { Text("Panoyu kaydet", fontWeight = FontWeight.Bold) } }
                }
            }
        }
    }
    pencere?.invoke()
}

@Composable
private fun PanoAyarlari(b: JSONObject, urunler: List<JSONObject>, renkler: List<JSONObject>, gorseller: List<JSONObject>, urunAd: Map<Int, JSONObject>, degis: (JSONObject.() -> Unit) -> Unit) {
    val tip = b.optString("tip")
    var urunAra by remember(b.optInt("i", -1), tip) { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (tip in listOf("etiket", "not", "baslik")) OutlinedTextField(b.optString("metin"), { y -> degis { put("metin", y) } }, label = { Text("Yazı") }, colors = alanRenkleri(), modifier = Modifier.fillMaxWidth())
        if (tip in listOf("kart", "combo", "vitrin")) {
            OutlinedTextField(b.optString("baslik"), { y -> degis { put("baslik", y) } }, label = { Text("Başlık") }, singleLine = true, colors = alanRenkleri(), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(b.optString("aciklama"), { y -> degis { put("aciklama", y) } }, label = { Text("Açıklama") }, colors = alanRenkleri(), modifier = Modifier.fillMaxWidth())
        }
        if (tip in listOf("fiyat", "tukendi", "kart", "combo", "vitrin", "daire")) {
            val u = urunAd[b.optInt("urun")]
            Text("Ürün: " + (u?.let { it.optString("ad") + " · " + tl(it.optDouble("fiyat")) + if (it.optBoolean("tukendi")) " · TÜKENDİ" else "" } ?: "seçilmedi"), fontWeight = FontWeight.SemiBold)
            Text("Ürün seçilirse fiyat menüden canlı gelir, “tükendi” işaretlenince TV’de çizilir.", color = Soluk, fontSize = 12.sp)
            OutlinedTextField(urunAra, { urunAra = it }, placeholder = { Text("Ürün ara…") }, singleLine = true, colors = alanRenkleri(), modifier = Modifier.fillMaxWidth())
            if (urunAra.length >= 2) urunler.filter { it.optString("ad").contains(urunAra, true) }.take(8).forEach { x ->
                Row(Modifier.fillMaxWidth().background(MaviAcik, RoundedCornerShape(10.dp)).clickable { degis { put("urun", x.optInt("id")) }; urunAra = "" }.padding(10.dp)) {
                    Text(x.optString("ad"), modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold); Text(tl(x.optDouble("fiyat")), color = Soluk)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(b.optString("fiyat").let { if (it == "null") "" else it }, { y -> degis { put("fiyat", y.filter { c -> c.isDigit() || c == ',' || c == '.' }) } }, label = { Text("Elle fiyat") },
                    singleLine = true, colors = alanRenkleri(), modifier = Modifier.weight(1f))
                if (u != null) TextButton(onClick = { degis { put("urun", JSONObject.NULL) } }) { Text("Ürünü kaldır", color = Kirmizi) }
            }
        }
        if (tip in listOf("etiket", "not", "baslik", "tablo", "fiyat")) Column {
            Text("Yazı boyutu: ${b.optInt("boyut", 50)}", fontSize = 13.sp)
            Slider(b.optInt("boyut", 50).toFloat(), { y -> degis { put("boyut", y.toInt()) } }, valueRange = 10f..240f)
        }
        if (tip == "etiket") Column {
            Text("Eğim: ${b.optInt("aci")}°", fontSize = 13.sp)
            Slider(b.optInt("aci").toFloat(), { y -> degis { put("aci", y.toInt()) } }, valueRange = -45f..45f)
        }
        if (tip in listOf("etiket", "not", "baslik", "panel", "kart", "combo", "vitrin")) Column {
            Text(if (tip == "not") "Yazı rengi" else "Renk", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            RenkSecici(renkler, b.optString("renk")) { r -> degis { put("renk", r) } }
        }
        if (tip == "etiket") Column {
            Text("Yazı rengi", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            RenkSecici(renkler, b.optString("yazi", "beyaz")) { r -> degis { put("yazi", r) } }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Cip2(b.optString("sekil") != "daire", "Kutu") { degis { put("sekil", "kutu") } }
                Cip2(b.optString("sekil") == "daire", "Daire") { degis { put("sekil", "daire") } }
            }
        }
        if (tip in listOf("resim", "kart")) Column {
            Text("Görsel", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item { Cip2(b.optString("resim").isEmpty(), "Yok") { degis { put("resim", ""); put("resim_url", "") } } }
                urunAd[b.optInt("urun")]?.takeIf { it.optBoolean("foto") }?.let { u -> item { Cip2(b.optString("resim") == "urun:" + u.optInt("id"), "Ürün fotoğrafı") { degis { put("resim", "urun:" + u.optInt("id")); put("resim_url", "") } } } }
                items(gorseller) { gx ->
                    Box(Modifier.size(64.dp).background(Zemin, RoundedCornerShape(8.dp)).border(if (b.optString("resim") == gx.optString("kod")) 3.dp else 0.dp, Turuncu, RoundedCornerShape(8.dp))
                        .clickable { degis { put("resim", gx.optString("kod")); put("resim_url", gx.optString("url")) } }) {
                        UzakResim(gx.optString("url"), Modifier.fillMaxSize().padding(4.dp))
                    }
                }
            }
        }
        val satirlar = b.optJSONArray("satirlar")
        if (satirlar != null && tip != "tablo") Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Satırlar", fontWeight = FontWeight.Bold)
            satirlar.nesneler().forEachIndexed { si, s ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(s.optString("ad"), { y -> degis { optJSONArray("satirlar")?.optJSONObject(si)?.put("ad", y) } }, label = { Text(urunAd[s.optInt("urun")]?.let { "→ " + it.optString("ad") } ?: "Satır") },
                        singleLine = true, colors = alanRenkleri(), modifier = Modifier.weight(1f))
                    OutlinedTextField(s.optString("fiyat").let { if (it == "null") "" else it }, { y -> degis { optJSONArray("satirlar")?.optJSONObject(si)?.put("fiyat", y.filter { c -> c.isDigit() || c == ',' || c == '.' }) } },
                        label = { Text("Fiyat") }, singleLine = true, colors = alanRenkleri(), modifier = Modifier.width(96.dp))
                }
            }
            Text("Ürüne bağlı satırların fiyatı menüden canlı gelir; elle fiyat yalnızca ürün bağlı değilse görünür.", color = Soluk, fontSize = 12.sp)
        }
        if (tip == "tablo") Text("Fiyat tablosunun hücreleri menüdeki ürünlere bağlıdır; fiyatlar ve “tükendi” durumları kendiliğinden güncellenir. Burada taşıyıp boyutlandırabilirsiniz.", color = Soluk, fontSize = 12.sp)
        Text("Konum: x ${b.optInt("x")} · y ${b.optInt("y")} · genişlik ${b.optInt("w")}" + if (b.has("h")) " · yükseklik ${b.optInt("h")}" else "", color = Soluk, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("←" to (-5 to 0), "→" to (5 to 0), "↑" to (0 to -5), "↓" to (0 to 5)).forEach { (ad, d) ->
                OutlinedButton(onClick = { degis { put("x", optInt("x") + d.first); put("y", optInt("y") + d.second) } }, contentPadding = PaddingValues(0.dp), modifier = Modifier.size(44.dp)) { Text(ad, fontSize = 18.sp) }
            }
            OutlinedButton(onClick = { degis { put("w", (optInt("w") - 10).coerceAtLeast(30)); if (optString("sekil") == "daire") put("h", optInt("w")) } }, contentPadding = PaddingValues(0.dp), modifier = Modifier.size(44.dp)) { Text("−", fontSize = 18.sp) }
            OutlinedButton(onClick = { degis { put("w", optInt("w") + 10); if (optString("sekil") == "daire") put("h", optInt("w")) } }, contentPadding = PaddingValues(0.dp), modifier = Modifier.size(44.dp)) { Text("+", fontSize = 18.sp) }
        }
    }
}

@Composable
private fun RenkSecici(renkler: List<JSONObject>, secili: String, sec: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
        renkler.forEach { r ->
            Box(Modifier.size(30.dp).background(renkCoz(r.optString("hex"), Color.Gray), CircleShape)
                .border(if (secili == r.optString("kod") || secili.equals(r.optString("hex"), true)) 3.dp else 1.dp, if (secili == r.optString("kod")) Color(0xFF2563EB) else Cizgi, CircleShape)
                .clickable { sec(r.optString("kod")) })
        }
    }
}

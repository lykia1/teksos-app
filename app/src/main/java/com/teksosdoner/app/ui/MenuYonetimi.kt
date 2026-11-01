@file:OptIn(ExperimentalMaterial3Api::class)

package com.teksosdoner.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teksosdoner.app.TR
import com.teksosdoner.app.kisaTarih
import com.teksosdoner.app.nesneler
import com.teksosdoner.app.tl
import kotlinx.coroutines.delay
import org.json.JSONObject

private fun sayi(d: Double): String = if (d % 1.0 == 0.0) d.toLong().toString() else String.format(TR, "%.2f", d).trimEnd('0').trimEnd(',')

/* ======================= ÜRÜNLER ======================= */
@Composable
fun UrunlerEkrani(geri: (() -> Unit)?, ac: (Int) -> Unit) {
    var aranan by rememberSaveable { mutableStateOf("") }
    var ara by remember { mutableStateOf("") }
    var kat by rememberSaveable { mutableStateOf("") }
    var durum by rememberSaveable { mutableStateOf("") }
    var recetesiz by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(aranan) { delay(400); ara = aranan.trim() }
    VeriEkrani(
        "Ürünler & Reçeteler", "y_urunler", geri,
        altBaslik = { v -> v.optJSONObject("sayilar")?.let { "${it.optInt("aktif")} aktif ürün · ${it.optInt("recetesiz")} reçetesiz" } },
        parametre = mapOf("ara" to ara, "kat" to kat, "durum" to durum, "recetesiz" to if (recetesiz) "1" else ""),
        ust = { v, _ ->
            Column(Modifier.padding(start = 14.dp, end = 14.dp, top = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(aranan, { aranan = it }, placeholder = { Text("Ürün adı veya barkod") }, singleLine = true, leadingIcon = { Icon(Icons.Filled.Search, null) },
                    trailingIcon = { if (aranan.isNotEmpty()) IconButton(onClick = { aranan = "" }) { Icon(Icons.Filled.Clear, "Temizle") } },
                    shape = RoundedCornerShape(14.dp), colors = alanRenkleri(), modifier = Modifier.fillMaxWidth())
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { Cip2(durum == "", "Aktif") { durum = "" } }
                    item { Cip2(durum == "pasif", "Pasif") { durum = "pasif" } }
                    item { Cip2(recetesiz, "Reçetesiz", v.optJSONObject("sayilar")?.optInt("recetesiz") ?: 0) { recetesiz = !recetesiz } }
                    item { Cip2(kat == "", "Tüm kategoriler") { kat = "" } }
                    items(v.optJSONArray("kategoriler").nesneler()) { c -> Cip2(kat == c.optInt("id").toString(), c.optString("ad")) { kat = c.optInt("id").toString() } }
                }
            }
        },
        fab = "Yeni ürün" to { v, k ->
            k.ac {
                FormPenceresi("Yeni ürün", listOf(
                    Alan("ad", "Ürün adı"),
                    Alan("kat_id", "Kategori", AlanTip.SECIM, secenekler = v.optJSONArray("kategoriler").nesneler().map { it.optInt("id").toString() to it.optString("ad") }),
                    Alan("fiyat", "Satış fiyatı (₺)", AlanTip.SAYI),
                    Alan("kdv", "KDV %", AlanTip.SAYI, "10"),
                ), kapat = k.kapat) { k.islem("y_urun_islem", govde("kaydet", 0, it)) }
            }
        }
    ) { v, k ->
        if (v.optBoolean("yemekpos")) item { Serit(Icons.Filled.SyncAlt, "Ürün adı, fiyat ve kategori YemekPOS'tan gelir; burada değiştirirseniz bir sonraki eşitlemede geri döner. Reçete ve maliyet burada düzenlenir.", Mavi, MaviAcik) }
        if (!v.optBoolean("merkez")) item { Serit(Icons.Filled.Warning, "Menü merkez şubeden düzenlenir; bu şubede yalnızca görüntülenir.", Sari, SariAcik) }
        val liste = v.optJSONArray("liste").nesneler()
        if (liste.isEmpty()) item { BosKutu("Ürün bulunamadı.") }
        items(liste, key = { it.optInt("id") }) { u ->
            Kart(onClick = { ac(u.optInt("id")) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).background(renkCoz(u.optString("renk"), Turuncu), CircleShape))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(u.optString("ad"), fontWeight = FontWeight.Bold, color = if (u.optBoolean("aktif")) Murekkep else Soluk)
                        Text(listOf(u.optString("kat").ifEmpty { "Kategorisiz" }, if (u.optInt("recete") > 0) "${u.optInt("recete")} malzeme" else null,
                            if (u.optDouble("maliyet") > 0) "maliyet " + tl(u.optDouble("maliyet")) else null).filterNotNull().joinToString(" · "), color = Soluk, fontSize = 12.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(tl(u.optDouble("fiyat")), fontWeight = FontWeight.ExtraBold)
                        when {
                            !u.optBoolean("aktif") -> Rozet("Pasif", Soluk, Color(0xFFF5F5F4))
                            u.optInt("recete") == 0 -> Rozet("Reçete yok", Sari, SariAcik)
                            else -> Rozet("marj %" + String.format(TR, "%.0f", u.optDouble("marj")), Yesil, YesilAcik)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UrunDetayEkrani(id: Int, geri: () -> Unit) {
    VeriEkrani("Ürün", "y_urun", geri, altBaslik = { it.optJSONObject("urun")?.optString("ad") }, parametre = mapOf("id" to id.toString())) { v, k ->
        val u = v.optJSONObject("urun") ?: JSONObject()
        val stok = v.optJSONArray("stok").nesneler()
        item {
            Kart {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(u.optString("ad"), fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                        Text(u.optString("kat").ifEmpty { "Kategorisiz" } + " · KDV %" + sayi(u.optDouble("kdv")), color = Soluk)
                    }
                    Switch(checked = u.optBoolean("aktif"), onCheckedChange = { k.islem("y_urun_islem", govde("durum", id)) }, colors = SwitchDefaults.colors(checkedTrackColor = Yesil))
                }
                Ayrac()
                BilgiSatir("Satış fiyatı", tl(u.optDouble("fiyat")), kalin = true)
                if (!u.isNull("fiyat_masa")) BilgiSatir("Masa fiyatı", tl(u.optDouble("fiyat_masa")))
                if (!u.isNull("fiyat_al")) BilgiSatir("Al götür fiyatı", tl(u.optDouble("fiyat_al")))
                if (!u.isNull("fiyat_paket")) BilgiSatir("Paket fiyatı", tl(u.optDouble("fiyat_paket")))
                BilgiSatir("Reçete maliyeti", tl(u.optDouble("maliyet")), renk = if (u.optDouble("maliyet") > 0) Murekkep else Sari)
                if (u.optString("aciklama").isNotEmpty()) Text(u.optString("aciklama"), color = Soluk, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
                EylemSatiri(
                    Triple("Ad / kategori", Mavi) {
                        k.ac {
                            FormPenceresi("Ürünü düzenle", listOf(
                                Alan("ad", "Ürün adı", deger = u.optString("ad")),
                                Alan("kat_id", "Kategori", AlanTip.SECIM, u.optInt("kat_id").toString(), v.optJSONArray("kategoriler").nesneler().map { it.optInt("id").toString() to it.optString("ad") }),
                                Alan("fiyat", "Satış fiyatı (₺)", AlanTip.SAYI, sayi(u.optDouble("fiyat"))),
                                Alan("kdv", "KDV %", AlanTip.SAYI, sayi(u.optDouble("kdv"))),
                                Alan("aciklama", "Açıklama (menüde görünür)", AlanTip.UZUN, u.optString("aciklama")),
                            ), kapat = k.kapat) { k.islem("y_urun_islem", govde("kaydet", id, it)) }
                        }
                    },
                    Triple("Fiyatlar", Turuncu) {
                        k.ac {
                            FormPenceresi("Fiyatlar", listOf(
                                Alan("fiyat", "Satış fiyatı (₺)", AlanTip.SAYI, sayi(u.optDouble("fiyat"))),
                                Alan("fiyat_masa", "Masa fiyatı", AlanTip.SAYI, if (u.isNull("fiyat_masa")) "" else sayi(u.optDouble("fiyat_masa")), ipucu = "Boş = satış fiyatı"),
                                Alan("fiyat_al", "Al götür fiyatı", AlanTip.SAYI, if (u.isNull("fiyat_al")) "" else sayi(u.optDouble("fiyat_al")), ipucu = "Boş = satış fiyatı"),
                                Alan("fiyat_paket", "Paket fiyatı", AlanTip.SAYI, if (u.isNull("fiyat_paket")) "" else sayi(u.optDouble("fiyat_paket")), ipucu = "Boş = satış fiyatı"),
                            ), kapat = k.kapat) { k.islem("y_urun_islem", govde("fiyat", id, it)) }
                        }
                    }
                )
            }
        }
        item {
            Kart {
                KartBaslik(Icons.Filled.MenuBook, "Reçete", "her satışta depodan düşer")
                val rec = u.optJSONArray("recete").nesneler()
                if (rec.isEmpty()) Text("Reçete girilmemiş; bu ürün satılınca depodan bir şey düşmez ve maliyeti 0 görünür.", color = Sari, fontSize = 13.sp)
                rec.forEach { r ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(r.optString("ad"), fontWeight = FontWeight.SemiBold)
                            Text(sayi(r.optDouble("miktar")) + " " + r.optString("birim") + " · " + tl(r.optDouble("tutar")) +
                                when (r.optString("kanal")) { "paket" -> " · yalnız paket/al götür"; "masa" -> " · yalnız masa"; else -> "" }, color = Soluk, fontSize = 12.sp)
                        }
                        androidx.compose.material3.TextButton(onClick = {
                            k.ac { OnayPenceresi("Satır silinsin mi?", r.optString("ad") + " reçeteden çıkarılacak.", "Sil", kapat = k.kapat) { k.islem("y_urun_islem", JSONObject().put("act", "recete_sil").put("id", id).put("rid", r.optInt("id"))) } }
                        }) { Text("Sil", color = Kirmizi) }
                    }
                }
                EylemSatiri(Triple("+ Malzeme ekle", Turuncu) {
                    k.ac {
                        FormPenceresi("Reçeteye malzeme", listOf(
                            Alan("stok_id", "Hammadde", AlanTip.SECIM, secenekler = stok.map { it.optInt("id").toString() to (it.optString("ad") + " (" + it.optString("birim") + ")") }),
                            Alan("miktar", "Miktar (hammadde biriminde)", AlanTip.SAYI, ipucu = "Örn. 0,15 KG"),
                            Alan("kanal", "Ne zaman düşsün?", AlanTip.SECIM, "", listOf("" to "Her satışta", "paket" to "Yalnız paket / al götür", "masa" to "Yalnız masada")),
                        ), kapat = k.kapat) { k.islem("y_urun_islem", govde("recete_ekle", id, it)) }
                    }
                })
            }
        }
        val gr = u.optJSONArray("gruplar")
        if (gr != null && gr.length() > 0) item {
            Kart {
                KartBaslik(Icons.Filled.PlaylistAdd, "Ekstra seçenek grupları", renk = Mavi)
                Text((0 until gr.length()).joinToString(", ") { gr.optString(it) }, fontSize = 14.sp)
            }
        }
    }
}

/* ======================= KATEGORİLER ======================= */
@Composable
fun KategorilerEkrani(geri: (() -> Unit)?) {
    val renkler = listOf("#E8590C" to "Turuncu", "#B91C1C" to "Kırmızı", "#C2410C" to "Kiremit", "#CA8A04" to "Hardal", "#15803D" to "Yeşil", "#1D4ED8" to "Mavi", "#7C3AED" to "Mor", "#DB2777" to "Pembe", "#57534E" to "Gri")
    VeriEkrani("Kategoriler", "y_kategoriler", geri, altBaslik = { "${it.optJSONArray("liste")?.length() ?: 0} kategori" },
        fab = "Kategori" to { _, k -> k.ac { FormPenceresi("Yeni kategori", listOf(Alan("ad", "Kategori adı"), Alan("renk", "Renk", AlanTip.SECIM, "#E8590C", renkler)), kapat = k.kapat) { k.islem("y_kategori_islem", govde("kaydet", 0, it)) } } }
    ) { v, k ->
        items(v.optJSONArray("liste").nesneler(), key = { it.optInt("id") }) { c ->
            Kart {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IkonRozet(Icons.Filled.Category, renkCoz(c.optString("renk"), Turuncu), 40)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(c.optString("ad"), fontWeight = FontWeight.Bold)
                        Text("${c.optInt("urun")} aktif ürün", color = Soluk, fontSize = 12.sp)
                    }
                    Switch(checked = c.optBoolean("aktif"), onCheckedChange = { k.islem("y_kategori_islem", govde("durum", c.optInt("id"))) }, colors = SwitchDefaults.colors(checkedTrackColor = Yesil))
                }
                EylemSatiri(Triple("Düzenle", Mavi) {
                    k.ac { FormPenceresi("Kategoriyi düzenle", listOf(Alan("ad", "Kategori adı", deger = c.optString("ad")), Alan("renk", "Renk", AlanTip.SECIM, c.optString("renk"), renkler)), kapat = k.kapat) {
                        k.islem("y_kategori_islem", govde("kaydet", c.optInt("id"), it)) } }
                })
            }
        }
    }
}

/* ======================= EKSTRA SEÇENEKLER ======================= */
@Composable
fun SeceneklerEkrani(geri: (() -> Unit)?) {
    VeriEkrani("Ekstra Seçenekler", "y_secenekler", geri, altBaslik = { "Sos, ekstra malzeme, boy seçimi" }) { v, k ->
        val gr = v.optJSONArray("gruplar").nesneler()
        if (gr.isEmpty()) item { BosKutu("Seçenek grubu yok.") }
        items(gr, key = { it.optInt("id") }) { g ->
            Kart {
                KartBaslik(Icons.Filled.PlaylistAdd, g.optString("ad"), (if (g.optInt("min") > 0) "zorunlu · " else "isteğe bağlı · ") + "${g.optInt("urun")} üründe", renk = Mavi)
                g.optJSONArray("secenekler").nesneler().forEach { o ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(o.optString("ad"), fontWeight = FontWeight.SemiBold, color = if (o.optBoolean("aktif")) Murekkep else Soluk)
                            Text((if (o.optInt("recete") > 0) "reçeteli" else "reçetesiz") + (if (o.optDouble("maliyet") > 0) " · maliyet " + tl(o.optDouble("maliyet")) else ""), color = Soluk, fontSize = 11.sp)
                        }
                        androidx.compose.material3.TextButton(onClick = {
                            k.ac { FormPenceresi(o.optString("ad"), listOf(Alan("fiyat", "Ek fiyat (₺)", AlanTip.SAYI, sayi(o.optDouble("fiyat")))), kapat = k.kapat) { k.islem("y_secenek_islem", govde("fiyat", o.optInt("id"), it)) } }
                        }) { Text("+" + tl(o.optDouble("fiyat")), fontWeight = FontWeight.Bold, color = Turuncu) }
                        Switch(checked = o.optBoolean("aktif"), onCheckedChange = { k.islem("y_secenek_islem", govde("durum", o.optInt("id"))) }, colors = SwitchDefaults.colors(checkedTrackColor = Yesil))
                    }
                }
            }
        }
    }
}

/* ======================= KAMPANYALAR ======================= */
@Composable
fun KampanyalarEkrani(geri: (() -> Unit)?) {
    VeriEkrani("Kampanyalar", "y_kampanyalar", geri, altBaslik = { if (it.optBoolean("sadakat")) "Sadakat programı açık" else "Sadakat programı kapalı" }) { v, k ->
        val l = v.optJSONArray("liste").nesneler()
        if (l.isEmpty()) item { BosKutu("Kampanya yok.") }
        items(l, key = { it.optInt("id") }) { c ->
            Kart {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IkonRozet(Icons.Filled.LocalOffer, if (c.optBoolean("aktif")) Color(0xFFDB2777) else Soluk, 40)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(c.optString("ad"), fontWeight = FontWeight.Bold)
                        Text(c.optString("tur_ad") + " · " + c.optString("deger"), color = Soluk, fontSize = 12.sp)
                    }
                    Switch(checked = c.optBoolean("aktif"), onCheckedChange = { k.islem("y_kampanya_islem", govde("durum", c.optInt("id"))) }, colors = SwitchDefaults.colors(checkedTrackColor = Yesil))
                }
                Spacer(Modifier.height(6.dp))
                val bilgi = listOfNotNull(
                    c.optString("kod").ifEmpty { null }?.let { "Kod: $it" },
                    if (c.optBoolean("otomatik")) "otomatik uygulanır" else null,
                    if (c.optString("bas").isNotEmpty() || c.optString("bit").isNotEmpty()) (c.optString("bas").let { if (it.length >= 10) kisaTarih(it) else "…" } + " – " + c.optString("bit").let { if (it.length >= 10) kisaTarih(it) else "…" }) else null,
                    c.optString("saat").ifEmpty { null },
                    if (c.optDouble("min") > 0) "min. sepet " + tl(c.optDouble("min")) else null,
                    "${c.optInt("kullanim")} kez kullanıldı" + if (c.optInt("limit") > 0) " / ${c.optInt("limit")}" else "",
                )
                Text(bilgi.joinToString(" · "), color = Soluk, fontSize = 12.sp)
                EylemSatiri(Triple("Sil", Kirmizi) {
                    k.ac { OnayPenceresi("Kampanya silinsin mi?", c.optString("ad") + " kalıcı olarak silinecek.", "Sil", kapat = k.kapat) { k.islem("y_kampanya_islem", govde("sil", c.optInt("id"))) } }
                })
            }
        }
        item { Text("Yeni kampanya kurmak (ürün seçimi, gün/saat kuralları) için kasadaki bilgisayardan Kampanyalar sayfasını kullanın; burada açıp kapatabilir ve silebilirsiniz.", color = Soluk, fontSize = 12.sp, modifier = Modifier.padding(4.dp)) }
    }
}

@file:OptIn(ExperimentalMaterial3Api::class)

package com.teksosdoner.app.ui

import com.teksosdoner.app.*

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teksosdoner.app.TR
import com.teksosdoner.app.gunAdi
import com.teksosdoner.app.kisaTarih
import com.teksosdoner.app.nesneler
import com.teksosdoner.app.tarihSaat
import com.teksosdoner.app.tl
import com.teksosdoner.app.json.JSONArray
import com.teksosdoner.app.json.JSONObject

internal fun JSONObject.dogru(k: String): Boolean = when (val v = opt(k)) {
    is Boolean -> v; is Number -> v.toInt() != 0; is String -> v.isNotEmpty() && v != "0"; else -> false
}

/* ======================= VARDİYA PLANI ======================= */
@Composable
fun VardiyaEkrani(geri: (() -> Unit)?) {
    var hafta by rememberSaveable { mutableIntStateOf(0) }
    VeriEkrani("Vardiya Planı", "y_vardiya", geri,
        altBaslik = { v -> v.optJSONArray("gunler")?.let { g -> kisaTarih(g.optString(0)) + " – " + kisaTarih(g.optString(6)) } },
        parametre = mapOf("hafta" to hafta.toString()),
        ust = { _, k ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { hafta-- }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Önceki hafta") }
                Text(when (hafta) { 0 -> "Bu hafta"; 1 -> "Gelecek hafta"; -1 -> "Geçen hafta"; else -> (if (hafta > 0) "+" else "") + "$hafta hafta" },
                    fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                IconButton(onClick = { hafta++ }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Sonraki hafta") }
                TextButton(onClick = {
                    k.ac { OnayPenceresi("Geçen haftayı kopyala", "Bir önceki haftanın vardiyaları bu haftaya kopyalanır; dolu günlere dokunulmaz.", "Kopyala", tehlikeli = false, kapat = k.kapat) {
                        k.islem("y_vardiya_islem", JSONObject().put("act", "kopyala").put("hafta", hafta)) } }
                }) { Icon(Icons.Filled.ContentCopy, null, Modifier.padding(end = 4.dp)); Text("Kopyala") }
            }
        },
        fab = "Vardiya ekle" to { v, k -> k.ac { vardiyaFormu(v, k, null, null) } }
    ) { v, k ->
        val gunler = v.optJSONArray("gunler").let { a -> (0 until (a?.length() ?: 0)).map { a!!.optString(it) } }
        val per = v.optJSONArray("personel").nesneler()
        if (per.isEmpty()) item { BosKutu("Aktif personel yok.") }
        items(per, key = { it.optInt("id") }) { p ->
            Kart {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(p.optString("ad"), fontWeight = FontWeight.Bold)
                        Text(p.optString("pozisyon"), color = Soluk, fontSize = 12.sp)
                    }
                    TextButton(onClick = { k.ac { vardiyaFormu(v, k, p.optInt("id"), null) } }) { Text("+ Ekle", color = Turuncu, fontWeight = FontWeight.Bold) }
                }
                Spacer(Modifier.height(6.dp))
                val pg = p.optJSONArray("gunler")
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    gunler.forEachIndexed { i, g ->
                        val x = pg?.optJSONObject(i)
                        val izin = x?.optBoolean("izin") == true
                        val zemin = when { x == null -> Color(0xFFF5F5F4); izin -> SariAcik; else -> YesilAcik }
                        Column(
                            Modifier.weight(1f).background(zemin, RoundedCornerShape(10.dp)).clickable {
                                if (x != null) k.ac {
                                    OnayPenceresi(p.optString("ad") + " · " + kisaTarih(g) + " " + gunAdi(g), x.optString("tip") + (if (x.optString("not").isNotEmpty()) "\n" + x.optString("not") else "") + "\n\nBu vardiya silinsin mi?", "Sil", kapat = k.kapat) {
                                        k.islem("y_vardiya_islem", govde("sil", x.optInt("id")))
                                    }
                                } else k.ac { vardiyaFormu(v, k, p.optInt("id"), g) }
                            }.padding(vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(gunAdi(g).take(3), fontSize = 10.sp, color = Soluk, fontWeight = FontWeight.SemiBold)
                            Text(g.takeLast(2), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(
                                when { x == null -> "—"; izin -> "İzin"; else -> x.optString("tip").substringBefore(" -").trim() },
                                fontSize = 10.sp, color = when { x == null -> Soluk; izin -> Sari; else -> Yesil }, fontWeight = FontWeight.Bold, maxLines = 1
                            )
                        }
                    }
                }
            }
        }
        item { Text("Boş güne dokunarak vardiya ekleyin; dolu güne dokunarak silin.", color = Soluk, fontSize = 12.sp, modifier = Modifier.padding(4.dp)) }
    }
}

@Composable
private fun vardiyaFormu(v: JSONObject, k: Kontrol, personel: Int?, gun: String?) {
    val gunler = v.optJSONArray("gunler").let { a -> (0 until (a?.length() ?: 0)).map { a!!.optString(it) } }
    FormPenceresi("Vardiya", listOf(
        Alan("personel_id", "Personel", AlanTip.SECIM, personel?.toString() ?: "", v.optJSONArray("personel").nesneler().map { it.optInt("id").toString() to it.optString("ad") }),
        Alan("gunler", "Günler", AlanTip.COKLU, gun ?: "", gunler.map { it to (gunAdi(it).take(3) + " " + it.takeLast(2)) }),
        Alan("tur", "Tür", AlanTip.SECIM, "calisma", listOf("calisma" to "Çalışma", "izin" to "İzinli / Tatil")),
        Alan("bas", "Başlangıç", AlanTip.SAAT, "11:00"),
        Alan("bit", "Bitiş", AlanTip.SAAT, "23:00"),
        Alan("not", "Not (isteğe bağlı)"),
    ), kapat = k.kapat) { m ->
        val g = JSONObject().put("act", "kaydet")
        m.forEach { (a, d) -> if (a == "gunler") g.put("gunler", JSONArray(d.split(",").filter { it.isNotEmpty() })) else g.put(a, d) }
        k.islem("y_vardiya_islem", g)
    }
}

/* ======================= MAAŞ & AVANS ======================= */
@Composable
fun MaasEkrani(geri: (() -> Unit)?) {
    var ay by rememberSaveable { mutableStateOf(buAy()) }
    VeriEkrani("Maaş & Avans", "y_maas", geri, altBaslik = { it.optString("ay_ad") }, parametre = mapOf("ay" to ay),
        ust = { _, _ -> AySecici(ay) { ay = it } },
        fab = "Avans ver" to { v, k ->
            k.ac {
                FormPenceresi("Avans ver", listOf(
                    Alan("personel_id", "Personel", AlanTip.SECIM, secenekler = v.optJSONArray("personel").nesneler().map { it.optInt("id").toString() to it.optString("ad") }),
                    Alan("tutar", "Tutar (₺)", AlanTip.SAYI),
                    Alan("tarih", "Tarih", AlanTip.TARIH, bugunTarih()),
                    Alan("odeme_yontemi", "Nereden ödendi?", AlanTip.SECIM, "nakit", listOf("nakit" to "Nakit (kasadan)", "kredi_karti" to "Kart / havale")),
                    Alan("aciklama", "Açıklama"),
                ), dugme = "Avansı işle", kapat = k.kapat) { k.islem("y_maas_islem", govde("avans", null, it)) }
            }
        }
    ) { v, k ->
        val t = v.optJSONObject("toplam") ?: JSONObject()
        item {
            KpiIzgara(listOf<@Composable (Modifier) -> Unit>(
                { m -> KpiKutu("Hakediş", tl(t.optDouble("brut")), Icons.Filled.Payments, Mavi, m) },
                { m -> KpiKutu("Verilen avans", tl(t.optDouble("avans")), Icons.Filled.Payments, Sari, m) },
                { m -> KpiKutu("Ödenecek", tl(t.optDouble("net")), Icons.Filled.CheckCircle, Yesil, m, koyu = true) },
            ))
        }
        item { BolumBaslik("Personel") }
        val l = v.optJSONArray("liste").nesneler()
        if (l.isEmpty()) item { BosKutu("Bu ay kayıt yok.") }
        items(l, key = { "p" + it.optInt("id") }) { p ->
            Kart {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(p.optString("ad"), fontWeight = FontWeight.Bold)
                        Text(when (p.optString("tip")) {
                            "saatlik" -> tl(p.optDouble("ucret")) + "/saat · " + String.format(TR, "%.1f", p.optDouble("onayli_saat")) + " sa onaylı" +
                                (if (p.optDouble("bekleyen_saat") > 0) " · " + String.format(TR, "%.1f", p.optDouble("bekleyen_saat")) + " sa bekliyor" else "")
                            else -> "Aylık " + tl(p.optDouble("ucret"))
                        }, color = Soluk, fontSize = 12.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(tl(p.optDouble("net")), fontWeight = FontWeight.ExtraBold, color = if (p.optDouble("net") < 0) Kirmizi else Murekkep)
                        if (p.optDouble("avans") > 0) Text("avans " + tl(p.optDouble("avans")), color = Sari, fontSize = 11.sp)
                    }
                }
            }
        }
        val av = v.optJSONArray("avanslar").nesneler()
        if (av.isNotEmpty()) {
            item { BolumBaslik("Verilen avanslar") }
            items(av, key = { "a" + it.optInt("id") }) { a ->
                Kart {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IkonRozet(Icons.Filled.Payments, Sari, 36)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(a.optString("ad"), fontWeight = FontWeight.SemiBold)
                            Text(kisaTarih(a.optString("tarih")) + (if (a.optString("aciklama").isNotEmpty()) " · " + a.optString("aciklama") else ""), color = Soluk, fontSize = 12.sp)
                        }
                        Text(tl(a.optDouble("tutar")), fontWeight = FontWeight.Bold)
                        TextButton(onClick = {
                            k.ac { OnayPenceresi("Avans silinsin mi?", a.optString("ad") + " · " + tl(a.optDouble("tutar")) + "\nBağlı gider fişi de silinir.", "Sil", kapat = k.kapat) {
                                k.islem("y_maas_islem", govde("avans_sil", a.optInt("id"))) } }
                        }) { Text("Sil", color = Kirmizi) }
                    }
                }
            }
        }
    }
}

/* ======================= HİJYEN ======================= */
@Composable
fun HijyenEkrani(geri: (() -> Unit)?) {
    VeriEkrani("Hijyen Kontrolleri", "y_hijyen", geri, altBaslik = { v ->
        val l = v.optJSONArray("liste").nesneler()
        "${l.count { !it.dogru("eksik") }}/${l.size} tamam · " + v.optString("gun").let { if (it.length >= 10) kisaTarih(it) else it }
    }) { v, k ->
        val l = v.optJSONArray("liste").nesneler()
        if (l.isEmpty()) item { BosKutu("Tanımlı hijyen kontrolü yok.") }
        items(l, key = { it.optInt("id") }) { c ->
            val tur = c.optString("tur")
            val eksik = c.dogru("eksik"); val uygunsuz = c.dogru("uygunsuz")
            Kart {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IkonRozet(when (tur) { "sicaklik" -> Icons.Filled.Thermostat; "foto" -> Icons.Filled.PhotoCamera; else -> Icons.Filled.CleaningServices },
                        when { uygunsuz -> Kirmizi; eksik -> Sari; else -> Yesil }, 40)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(c.optString("ad"), fontWeight = FontWeight.Bold)
                        Text(buildString {
                            append("${c.optInt("yapilan")}/${c.optInt("gunluk").coerceAtLeast(1)} bugün")
                            if (tur == "sicaklik") {
                                val mn = if (c.isNull("min")) null else c.optDouble("min"); val mx = if (c.isNull("max")) null else c.optDouble("max")
                                if (mn != null || mx != null) append(" · " + (mn?.let { String.format(TR, "%.0f", it) } ?: "…") + " / " + (mx?.let { String.format(TR, "%.0f", it) } ?: "…") + " " + c.optString("birim"))
                            }
                        }, color = Soluk, fontSize = 12.sp)
                    }
                    when {
                        uygunsuz -> Rozet("Uygunsuz", Kirmizi, KirmiziAcik)
                        eksik -> Rozet("Bekliyor", Sari, SariAcik)
                        else -> Icon(Icons.Filled.CheckCircle, "Tamam", tint = Yesil)
                    }
                }
                if (c.optString("aciklama").isNotEmpty()) Text(c.optString("aciklama"), color = Soluk, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                c.optJSONArray("kayitlar").nesneler().forEach { r ->
                    Text("• " + r.optString("saat") + " " + r.optString("kisi") + (if (!r.isNull("deger")) " · " + String.format(TR, "%.1f", r.optDouble("deger")) + " " + c.optString("birim") else "") +
                        (if (!r.optBoolean("uygun")) " · UYGUNSUZ" else "") + (if (r.optString("aciklama").isNotEmpty()) " · " + r.optString("aciklama") else ""),
                        fontSize = 12.sp, color = if (r.optBoolean("uygun")) Murekkep else Kirmizi)
                }
                when (tur) {
                    "foto" -> Text("Fotoğraflı kontrol: personel kendi telefonundan fotoğrafla işaretler.", color = Soluk, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                    "sicaklik" -> EylemSatiri(Triple("Sıcaklık gir", Turuncu) {
                        k.ac { FormPenceresi(c.optString("ad"), listOf(Alan("deger", "Ölçülen (" + c.optString("birim").ifEmpty { "°C" } + ")", AlanTip.SAYI), Alan("aciklama", "Not")), kapat = k.kapat) {
                            k.islem("y_hijyen_islem", govde(null, c.optInt("id"), it)) } }
                    })
                    else -> EylemSatiri(Triple("✓ Yapıldı", Yesil) { k.islem("y_hijyen_islem", govde(null, c.optInt("id"))) })
                }
            }
        }
    }
}

/* ======================= KAPANIŞ GÖREVLERİ ======================= */
@Composable
fun KapanisEkrani(geri: (() -> Unit)?) {
    VeriEkrani("Kapanış Görevleri", "y_kapanis", geri, altBaslik = { "Mesai bitirirken sorulan kontrol listesi" },
        fab = "Görev" to { v, k -> k.ac { kapanisFormu(v, k, null) } }
    ) { v, k ->
        val l = v.optJSONArray("liste").nesneler()
        if (l.isEmpty()) item { BosKutu("Görev yok.") }
        l.groupBy { it.optString("pozisyon_ad") }.forEach { (poz, gorevler) ->
            item(key = "b$poz") { BolumBaslik(poz) }
            items(gorevler, key = { it.optInt("id") }) { g ->
                Kart {
                    Text(g.optString("metin"), fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Rozet(g.optString("zaman_ad"), Mavi, MaviAcik)
                        if (g.optBoolean("foto")) Rozet("📷 Fotoğraf zorunlu", Turuncu, TuruncuAcik)
                    }
                    EylemSatiri(
                        Triple("Düzenle", Mavi) { k.ac { kapanisFormu(v, k, g) } },
                        Triple("Sil", Kirmizi) { k.ac { OnayPenceresi("Görev silinsin mi?", g.optString("metin"), "Sil", kapat = k.kapat) { k.islem("y_kapanis_islem", govde("sil", g.optInt("id"))) } } },
                    )
                }
            }
        }
    }
}

@Composable
private fun kapanisFormu(v: JSONObject, k: Kontrol, g: JSONObject?) {
    FormPenceresi(if (g == null) "Yeni görev" else "Görevi düzenle", listOf(
        Alan("metin", "Görev", AlanTip.UZUN, g?.optString("metin") ?: ""),
        Alan("pozisyon", "Kimlere?", AlanTip.SECIM, g?.optString("pozisyon") ?: "genel", v.optJSONArray("pozisyonlar").nesneler().map { it.optString("kod") to it.optString("ad") }),
        Alan("zaman", "Ne zaman?", AlanTip.SECIM, g?.optString("zaman") ?: "her_zaman", v.optJSONArray("zamanlar").nesneler().map { it.optString("kod") to it.optString("ad") }),
        Alan("foto", "Fotoğraf zorunlu", AlanTip.ANAHTAR, if (g?.optBoolean("foto") == true) "1" else ""),
    ), kapat = k.kapat) { k.islem("y_kapanis_islem", govde("kaydet", g?.optInt("id") ?: 0, it)) }
}

/* ======================= DUYURULAR ======================= */
@Composable
fun DuyurularEkrani(geri: (() -> Unit)?) {
    VeriEkrani("Duyurular", "y_duyurular", geri, altBaslik = { "Personele mesaj" },
        fab = "Duyuru yayınla" to { v, k ->
            k.ac {
                FormPenceresi("Yeni duyuru", listOf(
                    Alan("baslik", "Başlık"),
                    Alan("metin", "Mesaj", AlanTip.UZUN),
                    Alan("push", "Telefonlarına bildirim gönder", AlanTip.ANAHTAR, if (v.optBoolean("push")) "1" else "", ipucu = if (v.optBoolean("push")) "Uygulamayı kullanan tüm personele" else "Bildirim anahtarı kurulu değil"),
                ), dugme = "Yayınla", kapat = k.kapat) { k.islem("y_duyuru_islem", govde(null, null, it)) }
            }
        }
    ) { v, k ->
        val l = v.optJSONArray("liste").nesneler()
        if (l.isEmpty()) item { BosKutu("Henüz duyuru yok.") }
        items(l, key = { it.optInt("id") }) { d ->
            Kart {
                Row(verticalAlignment = Alignment.Top) {
                    IkonRozet(Icons.Filled.Campaign, Turuncu, 36)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(d.optString("baslik"), fontWeight = FontWeight.Bold)
                        Text(tarihSaat(d.optString("tarih")), color = Soluk, fontSize = 11.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(d.optString("metin"), fontSize = 14.sp)
                        if (d.optInt("toplam") > 0) {
                            Spacer(Modifier.height(6.dp))
                            Text("👁 ${d.optInt("okuyan")} / ${d.optInt("toplam")} kişi okudu", fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                color = if (d.optInt("okuyan") >= d.optInt("toplam")) Yesil else Sari)
                            val ok = d.optJSONArray("okumayan").metinler()
                            if (ok.isNotEmpty()) Text("Okumayan: " + ok.joinToString(", "), fontSize = 11.sp, color = Soluk)
                        }
                    }
                }
                EylemSatiri(Triple("Sil", Kirmizi) { k.ac { OnayPenceresi("Duyuru silinsin mi?", d.optString("baslik"), "Sil", kapat = k.kapat) { k.islem("y_duyuru_islem", govde("sil", d.optInt("id"))) } } })
            }
        }
    }
}

@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.teksosdoner.app.ui

import com.teksosdoner.app.*

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WorkHistory
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teksosdoner.app.json.JSONArray
import com.teksosdoner.app.json.JSONObject
import kotlinx.coroutines.launch

@OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)
private fun b64(s: String): ByteArray = kotlin.io.encoding.Base64.Default.decode(s)

/** Fotoğraf çek → sunucuya yükle → kimliğini ver (null: vazgeçildi / hata) */
private fun fotoYukle(kapsam: kotlinx.coroutines.CoroutineScope, hata: (String) -> Unit, sonuc: (String) -> Unit) {
    os.fotoCek(false) { b ->
        if (b == null) return@fotoCek
        kapsam.launch {
            try {
                sonuc(Api.postHam("mesai_foto", mapOf("tur" to "kapanis"), b64(b)).optString("id"))
            } catch (e: ApiHata) {
                hata(e.message ?: "Fotoğraf yüklenemedi.")
            }
        }
    }
}

private fun saatYaz(d: Double) = String.format(TR, "%.1f", d)

/* ======================= Aylık özet & takvim ======================= */

@Composable
fun OzetEkrani(geri: () -> Unit) {
    var ay by remember { mutableStateOf(buAy()) }
    VeriEkrani("Aylık özetim", "p_ozet", geri, parametre = mapOf("ay" to ay),
        ust = { _, _ -> Box(Modifier.padding(start = 14.dp, end = 14.dp, top = 10.dp)) { AySecici(ay) { ay = it } } }) { v, _ ->
        item {
            KpiIzgara(listOf<@Composable (Modifier) -> Unit>(
                { m -> KpiKutu("Çalışılan", saatYaz(v.optDouble("saat")) + " sa", Icons.Filled.AccessTime, Turuncu, m, alt = "${v.optInt("gun")} iş günü", koyu = true) },
                { m -> KpiKutu("Geç kalma", v.optInt("gec").toString(), Icons.Filled.Timer, if (v.optInt("gec") > 0) Kirmizi else Yesil, m,
                    alt = if (v.optInt("gec") > 0) "toplam ${v.optInt("gec_dk")} dk" else "${v.optInt("tolerans")} dk tolerans") },
                { m -> KpiKutu("Unutulan çıkış", v.optInt("unutulan").toString(), Icons.Filled.EventBusy, if (v.optInt("unutulan") > 0) Sari else Yesil, m) },
                { m -> KpiKutu("Görev / hijyen", "${v.optInt("gorev")} / ${v.optInt("hijyen")}", Icons.Filled.CheckCircle, Mavi, m, alt = "bu ay işaretlenen") },
            ))
        }
        v.optJSONObject("kurye")?.let { k ->
            item {
                Kart {
                    KartBaslik(Icons.Filled.TwoWheeler, "Kurye özetim", renk = Mavi)
                    BilgiSatir("Bugün teslim edilen", "${k.optInt("bugun_paket")} paket")
                    BilgiSatir("Bugün üzerimdeki nakit", tl(k.optDouble("bugun_nakit")))
                    BilgiSatir("Bugün gidilen yol (yaklaşık)", saatYaz(k.optDouble("bugun_km")) + " km")
                    Ayrac()
                    BilgiSatir("Bu ay teslim", "${k.optInt("ay_paket")} paket", kalin = true)
                    if (k.optDouble("paket_prim") > 0) BilgiSatir("Paket primi (${tl(k.optDouble("paket_prim"))} / paket)", tl(k.optDouble("ay_prim")), renk = Yesil, kalin = true)
                }
            }
        }
        item {
            Kart {
                KartBaslik(Icons.Filled.EmojiEvents, "Rozetlerim", renk = Color(0xFFCA8A04))
                v.optJSONArray("rozetler").nesneler().forEach { r ->
                    val var_ = r.optBoolean("kazandi")
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        IkonRozet(if (var_) Icons.Filled.EmojiEvents else Icons.Filled.Lock, if (var_) Color(0xFFCA8A04) else Color(0xFFA8A29E), 36)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(r.optString("ad"), fontWeight = FontWeight.Bold, color = if (var_) Murekkep else Soluk)
                            Text(r.optString("aciklama"), fontSize = 12.sp, color = Soluk)
                        }
                        if (var_) Rozet("Kazandın", Yesil, YesilAcik)
                    }
                }
            }
        }
        item {
            Kart {
                KartBaslik(Icons.Filled.WorkHistory, "Takvim", sag = v.optString("ay_ad"), renk = Turuncu)
                Takvim(v.optJSONArray("takvim").nesneler())
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Lejant(YesilAcik, "Onaylı"); Lejant(SariAcik, "Onay bekliyor"); Lejant(KirmiziAcik, "Geç / çıkış yok"); Lejant(MaviAcik, "İzin"); Lejant(Color(0xFFF5F5F4), "Planlı vardiya")
                }
            }
        }
        val gunler = v.optJSONArray("takvim").nesneler().filter { it.optDouble("saat") > 0 || it.optInt("gec_dk") > 0 || it.optBoolean("acik") }.reversed()
        if (gunler.isNotEmpty()) item {
            Kart {
                KartBaslik(Icons.Filled.AccessTime, "Gün gün")
                gunler.forEach { g ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(kisaTarih(g.optString("tarih")) + " " + gunAdi(g.optString("tarih")), fontWeight = FontWeight.SemiBold)
                            Text(listOfNotNull(
                                g.optString("giris").takeIf { it.isNotEmpty() }?.let { it + " → " + g.optString("cikis").ifEmpty { "?" } },
                                g.optString("vardiya").takeIf { it.isNotEmpty() }?.let { "vardiya $it" }
                            ).joinToString(" · "), color = Soluk, fontSize = 12.sp)
                        }
                        if (g.optInt("gec_dk") > 0) Rozet("${g.optInt("gec_dk")} dk geç", Kirmizi, KirmiziAcik, Modifier.padding(end = 6.dp))
                        if (g.optBoolean("acik")) Rozet("Çıkış yok", Sari, SariAcik, Modifier.padding(end = 6.dp))
                        Text(saatYaz(g.optDouble("saat")) + " sa", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun Lejant(renk: Color, ad: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(12.dp).background(renk, RoundedCornerShape(3.dp)).border(1.dp, Cizgi, RoundedCornerShape(3.dp)))
        Spacer(Modifier.width(4.dp))
        Text(ad, fontSize = 11.sp, color = Soluk)
    }
}

@Composable
private fun Takvim(gunler: List<JSONObject>) {
    if (gunler.isEmpty()) return
    val ilk = tarihParcala(gunler.first().optString("tarih")) ?: return
    val bosluk = haftaninGunu(ilk.first, ilk.second, ilk.third)
    val hucreler: List<JSONObject?> = List(bosluk) { null } + gunler
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz").forEach {
                Text(it, Modifier.weight(1f), fontSize = 10.sp, color = Soluk, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
            }
        }
        hucreler.chunked(7).forEach { hafta ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                (0 until 7).forEach { i ->
                    val g = hafta.getOrNull(i)
                    if (g == null) Spacer(Modifier.weight(1f).aspectRatio(0.8f))
                    else {
                        val saat = g.optDouble("saat")
                        val renk = when {
                            g.optString("izin") == "Onaylandı" || g.optString("vardiya") == "İzinli" -> MaviAcik
                            g.optInt("gec_dk") > 0 || g.optBoolean("acik") -> KirmiziAcik
                            saat > 0 && !g.optBoolean("onayli") -> SariAcik
                            saat > 0 -> YesilAcik
                            g.optString("vardiya").isNotEmpty() -> Color(0xFFF5F5F4)
                            else -> Color.White
                        }
                        Column(
                            Modifier.weight(1f).aspectRatio(0.8f).background(renk, RoundedCornerShape(8.dp)).border(1.dp, Cizgi, RoundedCornerShape(8.dp)).padding(2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                        ) {
                            Text(g.optString("tarih").takeLast(2).trimStart('0'), fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                color = if (g.optString("tarih") == bugunTarih()) Turuncu else Murekkep)
                            Text(if (saat > 0) saatYaz(saat) else if (g.optString("vardiya").isNotEmpty() && g.optString("vardiya") != "İzinli") g.optString("vardiya").take(5) else "",
                                fontSize = 9.sp, color = Soluk, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

/* ======================= Hak ediş geçmişi ======================= */

@Composable
fun HakedisGecmisEkrani(geri: () -> Unit) {
    VeriEkrani("Hak ediş geçmişim", "p_hakedis", geri, altBaslik = { "Son 6 ay" }) { v, _ ->
        val aylar = v.optJSONArray("aylar").nesneler()
        items(aylar.size) { i ->
            val a = aylar[i]
            Kart(koyu = i == 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(a.optString("ay_ad"), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, modifier = Modifier.weight(1f), color = if (i == 0) Color.White else Murekkep)
                    IconButton(onClick = {
                        os.paylas("Hak ediş · ${a.optString("ay_ad")}\nOnaylı mesai: ${saatYaz(a.optDouble("saat"))} saat\nBrüt: ${tl(a.optDouble("brut"))}\n" +
                            "Avans: −${tl(a.optDouble("avans"))}\nNet: ${tl(a.optDouble("net"))}")
                    }) { Icon(Icons.Filled.Share, "Paylaş", tint = if (i == 0) Color.White else Soluk) }
                }
                Text(tl(a.optDouble("net")), fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = if (i == 0) Color(0xFF86EFAC) else Yesil)
                Text("net hak ediş" + if (i == 0) " (şu ana kadar)" else "", fontSize = 12.sp, color = if (i == 0) Color(0xFFD6D3D1) else Soluk)
                Spacer(Modifier.height(8.dp))
                val c = if (i == 0) Color.White else Murekkep
                BilgiSatir("Onaylı mesai", saatYaz(a.optDouble("saat")) + " saat", renk = c)
                if (a.optDouble("bekleyen_saat") > 0) BilgiSatir("Onay bekleyen", saatYaz(a.optDouble("bekleyen_saat")) + " saat", renk = if (i == 0) Color(0xFFFDE68A) else Sari)
                BilgiSatir("Brüt", tl(a.optDouble("brut")), renk = c)
                BilgiSatir("Avans", "−" + tl(a.optDouble("avans")), renk = if (i == 0) Color(0xFFFCA5A5) else Kirmizi)
            }
        }
        val bordro = v.optJSONArray("bordrolar").nesneler()
        if (bordro.isNotEmpty()) item {
            Kart {
                KartBaslik(Icons.Filled.Payments, "Bordrolarım", renk = Yesil)
                bordro.forEach { b ->
                    BilgiSatir(b.optString("ay_ad"), tl(b.optDouble("net")), kalin = true,
                        alt = "${saatYaz(b.optDouble("saat"))} sa · brüt ${tl(b.optDouble("brut"))} · avans ${tl(b.optDouble("avans"))}" +
                            (if (b.optDouble("prim") > 0) " · prim ${tl(b.optDouble("prim"))}" else "") + " · " + b.optString("durum"))
                }
            }
        }
        val od = v.optJSONArray("odemeler").nesneler()
        if (od.isNotEmpty()) item {
            Kart {
                KartBaslik(Icons.Filled.Payments, "Avans ve ödemelerim", renk = Kirmizi)
                od.forEach { o -> BilgiSatir(kisaTarih(o.optString("tarih")) + "." + o.optString("tarih").take(4), tl(o.optDouble("tutar")), alt = o.optString("aciklama").takeIf { it.isNotEmpty() && it != "null" }) }
            }
        }
    }
}

/* ======================= Talepler ======================= */

@Composable
private fun TalepDurum(d: String, ad: String) = when (d) {
    "onaylandi", "okundu" -> Rozet(ad, Yesil, YesilAcik)
    "reddedildi" -> Rozet(ad, Kirmizi, KirmiziAcik)
    "iptal" -> Rozet(ad, Soluk, Color(0xFFF5F5F4))
    "acik" -> Rozet(ad, Mavi, MaviAcik)
    else -> Rozet(ad, Sari, SariAcik)
}

@Composable
fun TaleplerEkrani(geri: () -> Unit) {
    VeriEkrani("Taleplerim", "p_talepler", geri, altBaslik = { "Düzeltme, avans, vardiya devri, müsaitlik, mesaj" }) { v, k ->
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TalepDugme("Mesai düzeltme", Icons.Filled.AccessTime, Turuncu) {
                    k.ac {
                        FormPenceresi("Mesai düzeltme talebi", listOf(
                            Alan("tarih", "Gün", AlanTip.TARIH, bugunTarih()),
                            Alan("giris", "Giriş saati", AlanTip.SAAT, ""),
                            Alan("cikis", "Çıkış saati", AlanTip.SAAT, ""),
                            Alan("aciklama", "Ne oldu? (ör. çıkışı unuttum)", AlanTip.UZUN),
                        ), dugme = "Gönder", kapat = k.kapat) { k.islem("p_talep", govde("duzeltme", null, it)) }
                    }
                }
                if (v.optBoolean("avans_acik")) TalepDugme("Avans iste", Icons.Filled.Payments, Yesil) {
                    k.ac {
                        FormPenceresi("Avans talebi", listOf(Alan("tutar", "Tutar (₺)", AlanTip.SAYI), Alan("aciklama", "Açıklama (isteğe bağlı)", AlanTip.UZUN)),
                            dugme = "Gönder", kapat = k.kapat) { k.islem("p_talep", govde("avans", null, it)) }
                    }
                }
                val vard = v.optJSONArray("vardiyalar").nesneler()
                TalepDugme("Vardiya devret", Icons.Filled.SwapHoriz, Mavi) {
                    k.ac {
                        if (vard.isEmpty()) OnayPenceresi("Devredilecek vardiya yok", "Önümüzdeki 3 hafta için planlanmış vardiyanız görünmüyor.", "Tamam", tehlikeli = false, kapat = k.kapat) { k.kapat() }
                        else FormPenceresi("Vardiyamı devretmek istiyorum", listOf(
                            Alan("tarih", "Vardiya", AlanTip.SECIM, vard.first().optString("tarih"),
                                vard.map { it.optString("tarih") to kisaTarih(it.optString("tarih")) + " " + gunAdi(it.optString("tarih")) + " · " + it.optString("vardiya") }),
                            Alan("aciklama", "Neden? (iş arkadaşların görür)", AlanTip.UZUN),
                        ), dugme = "Duyur", kapat = k.kapat) { k.islem("p_talep", govde("takas", null, it)) }
                    }
                }
                TalepDugme("Yöneticiye yaz", Icons.Filled.Mail, Color(0xFF7C3AED)) {
                    k.ac {
                        FormPenceresi("Yöneticiye mesaj / öneri", listOf(
                            Alan("konu", "Konu", AlanTip.SECIM, "Öneri", listOf("Öneri" to "Öneri", "Şikayet" to "Şikayet", "Soru" to "Soru", "Diğer" to "Diğer")),
                            Alan("metin", "Mesajınız", AlanTip.UZUN),
                            Alan("isimsiz", "İsmim gösterilmesin", AlanTip.ANAHTAR, "", ipucu = "Yönetici mesajı kimin gönderdiğini görmez"),
                        ), dugme = "Gönder", kapat = k.kapat) { k.islem("p_talep", govde("mesaj", null, it)) }
                    }
                }
            }
        }
        val acik = v.optJSONArray("acik_takaslar").nesneler()
        if (acik.isNotEmpty()) item {
            Kart {
                KartBaslik(Icons.Filled.SwapHoriz, "Devredilen vardiyalar", sag = "almak isteyen alabilir", renk = Mavi)
                acik.forEach { t ->
                    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Text(t.optString("ad") + " · " + t.optString("ozet"), fontWeight = FontWeight.SemiBold)
                        if (t.optString("metin").isNotEmpty()) Text(t.optString("metin"), color = Soluk, fontSize = 12.sp)
                        OutlinedButton(onClick = {
                            k.ac { OnayPenceresi("Vardiyayı alıyor musunuz?", t.optString("ozet") + " vardiyası size geçecek (yönetici onayından sonra).", "Ben alırım", tehlikeli = false, kapat = k.kapat) {
                                k.islem("p_talep", govde("takas_al", t.optInt("id")))
                            } }
                        }, modifier = Modifier.padding(top = 4.dp)) { Text("Ben alırım", color = Mavi, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
        item {
            Kart {
                KartBaslik(Icons.Filled.EventBusy, "Müsaitliğim", sag = "önümüzdeki 14 gün", renk = Yesil)
                Text("Vardiya planı yapılırken yöneticin görür. Bir güne dokunup seç.", color = Soluk, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
                val ml = v.optJSONArray("musaitlik").nesneler()
                ml.chunked(7).forEach { hafta ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(bottom = 4.dp)) {
                        hafta.forEach { g ->
                            val d = g.optString("durum")
                            Column(
                                Modifier.weight(1f).background(when (d) { "musait" -> YesilAcik; "degil" -> KirmiziAcik; "yarim" -> SariAcik; else -> Color(0xFFF5F5F4) }, RoundedCornerShape(8.dp))
                                    .clickable {
                                        k.ac {
                                            FormPenceresi(kisaTarih(g.optString("tarih")) + " " + gunAdi(g.optString("tarih")), listOf(
                                                Alan("durum", "Durum", AlanTip.SECIM, d.ifEmpty { "musait" },
                                                    listOf("musait" to "Müsaitim", "yarim" to "Yarım gün", "degil" to "Müsait değilim", "temizle" to "Boş bırak")),
                                                Alan("not", "Not (isteğe bağlı)", AlanTip.METIN, g.optString("not")),
                                            ), kapat = k.kapat) { k.islem("p_talep", govde("musaitlik", null, it + ("tarih" to g.optString("tarih")))) }
                                        }
                                    }.padding(vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(gunAdi(g.optString("tarih")), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(g.optString("tarih").takeLast(2), fontSize = 12.sp)
                                Text(when (d) { "musait" -> "✓"; "degil" -> "✕"; "yarim" -> "½"; else -> "·" }, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                    color = when (d) { "musait" -> Yesil; "degil" -> Kirmizi; "yarim" -> Sari; else -> Soluk })
                            }
                        }
                    }
                }
            }
        }
        val tl_ = v.optJSONArray("talepler").nesneler()
        item { BolumBaslik("Gönderdiğim talepler") }
        if (tl_.isEmpty()) item { BosKutu("Henüz talep göndermediniz.") }
        items(tl_.size) { i ->
            val t = tl_[i]
            Kart {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(t.optString("tur_ad") + " · " + t.optString("ozet"), fontWeight = FontWeight.Bold)
                        Text(tarihSaat(t.optString("tarih")) + if (t.optString("ilgili").isNotEmpty()) " · alan: " + t.optString("ilgili") else "", color = Soluk, fontSize = 12.sp)
                    }
                    TalepDurum(t.optString("durum"), t.optString("durum_ad"))
                }
                if (t.optString("metin").isNotEmpty()) Text(t.optString("metin"), fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                if (t.optString("yanit").isNotEmpty()) Text("Yönetici: " + t.optString("yanit"), fontSize = 13.sp, color = Mavi, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
                if (t.optString("durum") in listOf("acik", "bekliyor")) EylemSatiri(Triple("Geri çek", Kirmizi) {
                    k.ac { OnayPenceresi("Talep geri çekilsin mi?", t.optString("tur_ad") + " · " + t.optString("ozet"), "Geri çek", kapat = k.kapat) { k.islem("p_talep", govde("iptal", t.optInt("id"))) } }
                })
            }
        }
    }
}

@Composable
private fun TalepDugme(ad: String, ikon: ImageVector, renk: Color, tikla: () -> Unit) {
    OutlinedButton(onClick = tikla, shape = RoundedCornerShape(12.dp)) {
        Icon(ikon, null, tint = renk, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(ad, color = Murekkep, fontWeight = FontWeight.SemiBold)
    }
}

/* ======================= Görevler & hijyen ======================= */

@Composable
fun GorevlerEkrani(geri: () -> Unit) {
    val kapsam = rememberCoroutineScope()
    var yukHata by remember { mutableStateOf<String?>(null) }
    VeriEkrani("Görevlerim", "p_gorevler", geri, altBaslik = { "Açılış, vardiya ortası ve hijyen kontrolleri" }) { v, k ->
        yukHata?.let { h -> item { MesajSeridi(h, true) { yukHata = null } } }
        val g = v.optJSONArray("gorevler").nesneler()
        item { BolumBaslik("Vardiya görevleri") }
        if (g.isEmpty()) item { BosKutu("Pozisyonunuz için açılış / vardiya ortası görevi tanımlanmamış.") }
        items(g.size) { i ->
            val x = g[i]
            Kart {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = x.optBoolean("yapildi"), colors = CheckboxDefaults.colors(checkedColor = Yesil), onCheckedChange = { yeni ->
                        when {
                            !yeni -> k.islem("p_gorev_islem", govde("geri", x.optInt("id")))
                            x.optBoolean("foto") -> fotoYukle(kapsam, { yukHata = it }) { f -> k.islem("p_gorev_islem", govde("gorev", x.optInt("id"), mapOf("foto_id" to f))) }
                            else -> k.islem("p_gorev_islem", govde("gorev", x.optInt("id")))
                        }
                    })
                    Column(Modifier.weight(1f)) {
                        Text(x.optString("metin"), fontWeight = FontWeight.SemiBold)
                        Text(x.optString("zaman") + (if (x.optBoolean("foto")) " · fotoğraflı" else "") + (if (x.optBoolean("yapildi")) " · " + x.optString("saat") + "'de yapıldı" else ""),
                            fontSize = 12.sp, color = if (x.optBoolean("yapildi")) Yesil else Soluk)
                    }
                    if (x.optBoolean("foto") && !x.optBoolean("yapildi")) Icon(Icons.Filled.PhotoCamera, null, tint = Turuncu)
                }
            }
        }
        val h = v.optJSONArray("hijyen").nesneler()
        item { BolumBaslik("Hijyen kontrolleri") }
        if (!v.optBoolean("mesaide")) item { Serit(Icons.Filled.Warning, "Hijyen kontrolleri mesaideyken görünür. Önce QR ile giriş yapın.", Sari, SariAcik) }
        else if (h.isEmpty()) item { BosKutu("Bugün sizin için bekleyen hijyen kontrolü yok. 👍") }
        items(h.size) { i ->
            val x = h[i]
            Kart {
                Text(x.optString("ad"), fontWeight = FontWeight.Bold)
                val sinir = if (x.optString("tur") == "sicaklik") " · olmalı: ${if (x.isNull("min")) "…" else saatYaz(x.optDouble("min"))} – ${if (x.isNull("max")) "…" else saatYaz(x.optDouble("max"))} ${x.optString("birim")}" else ""
                Text("Bugün ${x.optInt("eksik")} kez daha" + sinir + if (x.optString("aciklama").isNotEmpty()) " · " + x.optString("aciklama") else "", fontSize = 12.sp, color = Soluk)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    when (x.optString("tur")) {
                        "sicaklik" -> Button(onClick = {
                            k.ac { FormPenceresi(x.optString("ad"), listOf(Alan("deger", "Ölçülen (${x.optString("birim")})", AlanTip.SAYI), Alan("aciklama", "Not (isteğe bağlı)")), kapat = k.kapat) {
                                k.islem("p_gorev_islem", govde("hijyen", x.optInt("id"), it))
                            } }
                        }, colors = ButtonDefaults.buttonColors(containerColor = Turuncu)) { Text("Değeri gir") }
                        "foto" -> Button(onClick = { fotoYukle(kapsam, { yukHata = it }) { f -> k.islem("p_gorev_islem", govde("hijyen", x.optInt("id"), mapOf("foto_id" to f))) } },
                            colors = ButtonDefaults.buttonColors(containerColor = Turuncu)) { Icon(Icons.Filled.PhotoCamera, null); Spacer(Modifier.width(6.dp)); Text("Fotoğraf çek") }
                        else -> {
                            Button(onClick = { k.islem("p_gorev_islem", govde("hijyen", x.optInt("id"))) }, colors = ButtonDefaults.buttonColors(containerColor = Yesil)) { Text("Uygun") }
                            OutlinedButton(onClick = {
                                k.ac { FormPenceresi("Uygun değil: " + x.optString("ad"), listOf(Alan("aciklama", "Ne sorun var?", AlanTip.UZUN)), renk = Kirmizi, kapat = k.kapat) {
                                    k.islem("p_gorev_islem", govde("hijyen", x.optInt("id"), it + ("uygunsuz" to "1")))
                                } }
                            }) { Text("Uygun değil", color = Kirmizi) }
                        }
                    }
                }
            }
        }
    }
}

/* ======================= Fire ======================= */

@Composable
fun FireEkrani(geri: () -> Unit) {
    VeriEkrani("Fire / zayi bildir", "p_fire", geri, altBaslik = { "Yanan, düşen, bozulan ürün" },
        fab = "Fire bildir" to { v, k ->
            val u = v.optJSONArray("urunler").nesneler()
            k.ac {
                FormPenceresi("Fire bildir", listOf(
                    Alan("urun_id", "Ürün", AlanTip.SECIM, "", u.map { it.optInt("id").toString() to it.optString("ad") + " (" + it.optString("birim") + ")" }),
                    Alan("miktar", "Miktar", AlanTip.SAYI, ipucu = "Ürünün birimiyle (kg, adet…)"),
                    Alan("sebep", "Ne oldu?", AlanTip.SECIM, "Yandı", listOf("Yandı" to "Yandı", "Yere düştü" to "Yere düştü", "Bozuldu / tarihi geçti" to "Bozuldu / tarihi geçti",
                        "Müşteri iade" to "Müşteri iade", "Hatalı hazırlandı" to "Hatalı hazırlandı", "Diğer" to "Diğer")),
                ), dugme = "Kaydet", renk = Kirmizi, kapat = k.kapat) { k.islem("p_fire_kaydet", govde(null, null, it)) }
            }
        }) { v, _ ->
        item { Serit(Icons.Filled.DeleteSweep, "Bildirdiğiniz fire stoktan düşülür ve yöneticiye iletilir. Doğru stok için her fireyi girin.", Mavi, MaviAcik) }
        val s = v.optJSONArray("son").nesneler()
        if (s.isEmpty()) item { BosKutu("Henüz fire bildirmediniz.") }
        items(s.size) { i ->
            val f = s[i]
            Kart { BilgiSatir(f.optString("urun"), String.format(TR, "%s %s", ondalikYaz(f.optDouble("miktar"), 2, TR), f.optString("birim")), kalin = true, alt = tarihSaat(f.optString("tarih")) + " · " + f.optString("sebep")) }
        }
    }
}

/* ======================= Vardiya sonu sayımı ======================= */

@Composable
fun SayimEkrani(geri: () -> Unit) {
    val sayim = remember { mutableStateMapOf<Int, String>() }
    val doner = remember { mutableStateMapOf<Int, String>() }
    var ara by remember { mutableStateOf("") }
    VeriEkrani("Vardiya sayımı", "p_sayim", geri, altBaslik = { "Vardiya sonunda elde kalanı say" }) { v, k ->
        item { Serit(Icons.Filled.Inventory, "Saydığınız miktarları yazın; boş bıraktıklarınız kaydedilmez. Sistemdekinden farklı çıkanlar yöneticiye iletilir.", Mavi, MaviAcik) }
        val d = v.optJSONArray("doner").nesneler()
        if (d.isNotEmpty()) {
            item { BolumBaslik("Döner şişleri (artan kg)") }
            items(d.size) { i ->
                val x = d[i]
                Kart {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(x.optString("ad"), fontWeight = FontWeight.Bold)
                            Text("Takılan: ${ondalikYaz(x.optDouble("takilan"), 1, TR)} kg" + if (!x.isNull("artan")) " · kayıtlı artan: ${ondalikYaz(x.optDouble("artan"), 1, TR)} kg" else "", color = Soluk, fontSize = 12.sp)
                        }
                        SayiKutusu(doner[x.optInt("id")].orEmpty(), "kg") { doner[x.optInt("id")] = it }
                    }
                }
            }
        }
        item {
            BolumBaslik(if (v.optBoolean("secili")) "Sayılacak ürünler" else "Ürünler")
            OutlinedTextField(ara, { ara = it }, label = { Text("Ürün ara") }, singleLine = true, colors = alanRenkleri(), modifier = Modifier.fillMaxWidth())
        }
        val u = v.optJSONArray("urunler").nesneler().filter { ara.isBlank() || it.optString("ad").lowercase(TR).contains(ara.lowercase(TR)) }
        items(u.size) { i ->
            val x = u[i]
            Row(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(x.optString("ad"), Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                SayiKutusu(sayim[x.optInt("id")].orEmpty(), x.optString("birim")) { sayim[x.optInt("id")] = it }
            }
        }
        item {
            val dolu = sayim.count { it.value.isNotBlank() } + doner.count { it.value.isNotBlank() }
            Button(onClick = {
                val g = JSONObject()
                    .put("sayimlar", JSONArray(sayim.filter { it.value.isNotBlank() }.map { (id, m) -> JSONObject().put("id", id).put("miktar", m) }))
                    .put("doner", JSONArray(doner.filter { it.value.isNotBlank() }.map { (id, m) -> JSONObject().put("id", id).put("artan", m) }))
                k.islem("p_sayim_kaydet", g)
                sayim.clear(); doner.clear()
            }, enabled = dolu > 0, colors = ButtonDefaults.buttonColors(containerColor = Turuncu), shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(54.dp)) { Text(if (dolu > 0) "$dolu kalemi kaydet" else "Miktar girin", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun SayiKutusu(deger: String, birim: String, degis: (String) -> Unit) {
    OutlinedTextField(deger, { degis(it.filter { c -> c.isDigit() || c == ',' || c == '.' }.take(10)) }, singleLine = true,
        suffix = { Text(birim, fontSize = 11.sp, color = Soluk) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        colors = alanRenkleri(), modifier = Modifier.width(130.dp))
}

/* ======================= Rehber ======================= */

@Composable
fun RehberEkrani(geri: () -> Unit) {
    VeriEkrani("İş arkadaşlarım", "p_rehber", geri, altBaslik = { v -> "${v.optJSONArray("kisiler").nesneler().count { it.optBoolean("mesaide") }} kişi şu an mesaide" }) { v, _ ->
        val l = v.optJSONArray("kisiler").nesneler().sortedByDescending { it.optBoolean("mesaide") }
        items(l.size) { i ->
            val p = l[i]
            Kart {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        IkonRozet(Icons.Filled.Person, if (p.optBoolean("yonetici")) Turuncu else Mavi, 40)
                        if (p.optBoolean("mesaide")) Box(Modifier.align(Alignment.BottomEnd).size(12.dp).background(Color(0xFF22C55E), CircleShape).border(2.dp, Color.White, CircleShape))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(p.optString("ad"), fontWeight = FontWeight.Bold)
                        Text(p.optString("pozisyon") + (if (p.optBoolean("yonetici")) " · yönetici" else "") + (if (p.optBoolean("mesaide")) " · şu an mesaide" else ""),
                            fontSize = 12.sp, color = if (p.optBoolean("mesaide")) Yesil else Soluk)
                    }
                    if (p.optString("telefon").length >= 10) IconButton(onClick = { os.telefonAra("+" + p.optString("telefon").let { t -> if (t.startsWith("90")) t else "90" + t.takeLast(10) }) }) {
                        Icon(Icons.Filled.Call, "Ara", tint = Yesil)
                    }
                }
            }
        }
    }
}

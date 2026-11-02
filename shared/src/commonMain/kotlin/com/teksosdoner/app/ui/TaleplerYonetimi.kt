@file:OptIn(ExperimentalLayoutApi::class)

package com.teksosdoner.app.ui

import com.teksosdoner.app.*

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teksosdoner.app.json.JSONObject

/** Yönetici: personel talepleri (düzeltme, avans, vardiya devri, mesaj), müsaitlik, fire, sayım, ayarlar. */
@Composable
fun TaleplerYonetimEkrani(geri: () -> Unit) {
    var sekme by remember { mutableStateOf("bekleyen") }
    VeriEkrani("Personel talepleri", "y_talepler", geri, altBaslik = { v -> "${v.optJSONArray("bekleyen").nesneler().size} talep onay bekliyor" },
        ust = { v, k ->
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(start = 14.dp, end = 14.dp, top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Cip2(sekme == "bekleyen", "Bekleyen", v.optJSONArray("bekleyen").nesneler().size) { sekme = "bekleyen" }
                Cip2(sekme == "musait", "Müsaitlik") { sekme = "musait" }
                Cip2(sekme == "fire", "Fire") { sekme = "fire" }
                Cip2(sekme == "sayim", "Sayım", v.optJSONArray("sayimlar").nesneler().count { !it.optBoolean("uygulandi") && (it.optDouble("fark").let { f -> !f.isNaN() && kotlin.math.abs(f) >= 0.01 }) }) { sekme = "sayim" }
                Cip2(sekme == "gecmis", "Geçmiş") { sekme = "gecmis" }
                Cip2(false, "⚙ Ayarlar") { ayarAc(v, k) }
            }
        }) { v, k ->
        when (sekme) {
            "bekleyen" -> {
                val b = v.optJSONArray("bekleyen").nesneler()
                if (b.isEmpty()) item { BosKutu("Onay bekleyen talep yok. 👍") }
                items(b.size) { i -> TalepKarti(b[i], k, true) }
                val a = v.optJSONArray("acik_takaslar").nesneler()
                if (a.isNotEmpty()) {
                    item { BolumBaslik("Devralacak kişi arayan vardiyalar") }
                    items(a.size) { i -> TalepKarti(a[i], k, true) }
                }
            }
            "gecmis" -> {
                val g = v.optJSONArray("gecmis").nesneler()
                if (g.isEmpty()) item { BosKutu("Henüz sonuçlanan talep yok.") }
                items(g.size) { i -> TalepKarti(g[i], k, false) }
            }
            "musait" -> {
                val gunler = v.optJSONArray("gunler").metinler()
                val m = v.optJSONArray("musaitlik").nesneler()
                item { Serit(Icons.Filled.SwapHoriz, "Personelin bildirdiği müsaitlik ve planlı vardiyalar (önümüzdeki 14 gün). ✓ müsait · ½ yarım gün · ✕ müsait değil · saat = planlı vardiya", Mavi, MaviAcik) }
                if (m.isEmpty()) item { BosKutu("Henüz kimse müsaitlik bildirmedi.") }
                items(m.size) { i ->
                    val p = m[i]
                    Kart {
                        Text(p.optString("ad"), fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            p.optJSONArray("gunler").nesneler().forEach { g ->
                                val d = g.optString("durum")
                                val vard = g.optString("vardiya")
                                Column(
                                    Modifier.width(52.dp).background(when (d) { "musait" -> YesilAcik; "degil" -> KirmiziAcik; "yarim" -> SariAcik; else -> Color(0xFFF5F5F4) }, RoundedCornerShape(8.dp))
                                        .padding(vertical = 5.dp, horizontal = 2.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(gunAdi(g.optString("tarih")) + " " + g.optString("tarih").takeLast(2), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Text(when (d) { "musait" -> "✓"; "degil" -> "✕"; "yarim" -> "½"; else -> "·" }, fontWeight = FontWeight.Bold,
                                        color = when (d) { "musait" -> Yesil; "degil" -> Kirmizi; "yarim" -> Sari; else -> Soluk })
                                    Text(if (vard.startsWith("İzin")) "izin" else vard.take(5), fontSize = 9.sp, color = Mavi, textAlign = TextAlign.Center, maxLines = 1)
                                }
                            }
                        }
                        val notlar = p.optJSONArray("gunler").nesneler().filter { it.optString("not").isNotEmpty() }
                        notlar.forEach { Text(kisaTarih(it.optString("tarih")) + ": " + it.optString("not"), fontSize = 12.sp, color = Soluk) }
                    }
                }
            }
            "fire" -> {
                val f = v.optJSONArray("fire").nesneler()
                if (f.isEmpty()) item { BosKutu("Son 30 günde fire bildirilmedi.") }
                else item {
                    val top = f.groupBy { it.optString("urun") + " (" + it.optString("birim") + ")" }.mapValues { (_, l) -> l.sumOf { it.optDouble("miktar") } }
                        .entries.sortedByDescending { it.value }.take(5)
                    Kart {
                        KartBaslik(Icons.Filled.Settings, "En çok fire (30 gün)", renk = Kirmizi)
                        top.forEach { (u, m) -> BilgiSatir(u, ondalikYaz(m, 2, TR)) }
                    }
                }
                items(f.size) { i ->
                    val x = f[i]
                    Kart { BilgiSatir(x.optString("urun"), ondalikYaz(x.optDouble("miktar"), 2, TR) + " " + x.optString("birim"), kalin = true,
                        alt = tarihSaat(x.optString("tarih")) + " · " + x.optString("ad") + if (x.optString("sebep").isNotEmpty()) " · " + x.optString("sebep") else "") }
                }
            }
            "sayim" -> {
                val s = v.optJSONArray("sayimlar").nesneler()
                item { Serit(Icons.Filled.Settings, "Personelin vardiya sonu sayımları. Doğru bulduğunuzu \"Stoğa işle\" ile stoğa yazın.", Mavi, MaviAcik) }
                if (s.isEmpty()) item { BosKutu("Son 14 günde sayım girilmedi.") }
                items(s.size) { i ->
                    val x = s[i]
                    val fark = x.optDouble("fark")
                    Kart {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(x.optString("urun"), fontWeight = FontWeight.Bold)
                                Text(tarihSaat(x.optString("tarih")) + " · " + x.optString("ad"), fontSize = 12.sp, color = Soluk)
                                Text("Sayılan ${ondalikYaz(x.optDouble("miktar"), 2, TR)} · sistem ${if (x.isNull("beklenen")) "—" else ondalikYaz(x.optDouble("beklenen"), 2, TR)} ${x.optString("birim")}",
                                    fontSize = 12.sp)
                            }
                            if (!fark.isNaN() && kotlin.math.abs(fark) >= 0.01)
                                Rozet((if (fark > 0) "+" else "") + ondalikYaz(fark, 2, TR), if (fark < 0) Kirmizi else Sari, if (fark < 0) KirmiziAcik else SariAcik)
                            else Rozet("Tutuyor", Yesil, YesilAcik)
                        }
                        if (x.optBoolean("uygulandi")) Text("Stoğa işlendi", color = Yesil, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
                        else EylemSatiri(Triple("Stoğa işle", Turuncu) {
                            k.ac { OnayPenceresi("Sayım stoğa işlensin mi?", x.optString("urun") + " stoğu bu sayıma göre güncellenecek (sonraki satış ve fireler düşülerek).", "İşle", tehlikeli = false, kapat = k.kapat) {
                                k.islem("y_talep_islem", govde("sayim_uygula", x.optInt("id")))
                            } }
                        })
                    }
                }
            }
        }
    }
}

private fun ayarAc(v: JSONObject, k: Kontrol) {
    val a = v.optJSONObject("ayarlar") ?: JSONObject()
    val bir = { ad: String -> if (a.optBoolean(ad)) "1" else "" }
    k.ac {
        FormPenceresi("Personel paneli ayarları", listOf(
            Alan("gec_tolerans", "Geç kalma toleransı (dakika)", AlanTip.SAYI, a.optString("gec_tolerans"), ipucu = "Vardiya başlangıcından bu kadar sonrası geç sayılır"),
            Alan("hatirlatma", "Vardiya ve çıkış hatırlatmaları", AlanTip.ANAHTAR, bir("hatirlatma"), ipucu = "Vardiyadan 1 saat önce ve unutulan çıkışta telefona bildirim"),
            Alan("avans_talebi", "Uygulamadan avans talebi", AlanTip.ANAHTAR, bir("avans_talebi")),
            Alan("rehber_telefon", "Rehberde telefonlar görünsün", AlanTip.ANAHTAR, bir("rehber_telefon"), ipucu = "Kapalıysa telefonları yalnızca yöneticiler görür"),
            Alan("fire_bildirim", "Fire bildirimi gelsin", AlanTip.ANAHTAR, bir("fire_bildirim")),
            Alan("kurye_prim", "Kurye paket primi (₺ / paket)", AlanTip.SAYI, a.optString("kurye_prim"), ipucu = "0 ise kuryeye prim gösterilmez"),
            Alan("sayim_urunleri", "Vardiya sonu sayılacak ürünler", AlanTip.COKLU, a.optString("sayim_urunleri"),
                v.optJSONArray("stok_urunler").nesneler().map { it.optInt("id").toString() to it.optString("ad") }, ipucu = "Hiç seçilmezse tüm ürünler listelenir"),
        ), kapat = k.kapat) { d ->
            k.islem("y_talep_islem", govde("ayar", null, d.mapValues { (ad, deger) ->
                if (ad in listOf("hatirlatma", "avans_talebi", "rehber_telefon", "fire_bildirim")) (if (deger == "1") "1" else "0") else deger
            }))
        }
    }
}

@Composable
private fun TalepKarti(t: JSONObject, k: Kontrol, islem: Boolean) {
    Kart {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(t.optString("ad") + " · " + t.optString("tur_ad"), fontWeight = FontWeight.Bold)
                Text(t.optString("ozet"), fontWeight = FontWeight.SemiBold, color = Turuncu)
                Text(tarihSaat(t.optString("tarih")) + if (t.optString("ilgili").isNotEmpty()) " · devralan: " + t.optString("ilgili") else "", fontSize = 12.sp, color = Soluk)
            }
            when (t.optString("durum")) {
                "onaylandi", "okundu" -> Rozet(t.optString("durum_ad"), Yesil, YesilAcik)
                "reddedildi" -> Rozet(t.optString("durum_ad"), Kirmizi, KirmiziAcik)
                "iptal" -> Rozet(t.optString("durum_ad"), Soluk, Color(0xFFF5F5F4))
                "acik" -> Rozet(t.optString("durum_ad"), Mavi, MaviAcik)
                else -> Rozet(t.optString("durum_ad"), Sari, SariAcik)
            }
        }
        if (t.optString("metin").isNotEmpty()) Text(t.optString("metin"), fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp))
        if (t.optString("yanit").isNotEmpty()) Text("Yanıt: " + t.optString("yanit"), fontSize = 13.sp, color = Mavi, modifier = Modifier.padding(top = 4.dp))
        if (islem) {
            val tur = t.optString("tur")
            val acik = t.optString("durum") == "acik"
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!acik) Button(onClick = {
                    k.ac {
                        FormPenceresi(if (tur == "mesaj") "Okundu olarak işaretle" else "Onayla: " + t.optString("tur_ad"), listOf(
                            Alan("yanit", if (tur == "mesaj") "Yanıtınız (isteğe bağlı, personele bildirim gider)" else "Not (isteğe bağlı)", AlanTip.UZUN)
                        ), dugme = if (tur == "mesaj") "Tamam" else "Onayla", renk = Yesil, kapat = k.kapat) {
                            k.islem("y_talep_islem", govde("onayla", t.optInt("id"), it))
                        }
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = Yesil)) {
                    Text(when (tur) { "mesaj" -> "Okudum"; "avans" -> "Onayla · avans yaz"; "duzeltme" -> "Onayla · mesaiye işle"; "takas" -> "Onayla · vardiyayı devret"; else -> "Onayla" })
                }
                if (tur != "mesaj") OutlinedButton(onClick = {
                    k.ac {
                        FormPenceresi("Reddet: " + t.optString("tur_ad"), listOf(Alan("yanit", "Neden? (personele gider)", AlanTip.UZUN)), dugme = "Reddet", renk = Kirmizi, kapat = k.kapat) {
                            k.islem("y_talep_islem", govde("reddet", t.optInt("id"), it))
                        }
                    }
                }) { Text("Reddet", color = Kirmizi) }
            }
        }
    }
}

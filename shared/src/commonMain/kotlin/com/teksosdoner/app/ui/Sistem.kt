@file:OptIn(ExperimentalMaterial3Api::class)

package com.teksosdoner.app.ui

import com.teksosdoner.app.*

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teksosdoner.app.TR
import com.teksosdoner.app.nesneler
import com.teksosdoner.app.tarihSaat
import com.teksosdoner.app.tl
import com.teksosdoner.app.json.JSONObject

private fun oran(d: Double) = "%" + String.format(TR, "%.2f", d).trimEnd('0').trimEnd(',')
private fun oranYaz(d: Double) = String.format(TR, "%.2f", d).trimEnd('0').trimEnd(',')

/* ======================= ÖDEME YÖNTEMLERİ & KANALLAR ======================= */
@Composable
fun OdemeEkrani(geri: (() -> Unit)?) {
    VeriEkrani("Ödeme & Komisyon", "y_odeme", geri, altBaslik = { "Kart ve platform kesintileri kâra yansır" }) { v, k ->
        fun satir(x: JSONObject, tip: String) {
            k.ac {
                FormPenceresi(x.optString("ad"), listOf(
                    Alan("komisyon", "Komisyon oranı (%)", AlanTip.SAYI, oranYaz(x.optDouble("komisyon"))),
                    Alan("aktif", "Kasada görünsün", AlanTip.ANAHTAR, if (x.optBoolean("aktif")) "1" else ""),
                ), kapat = k.kapat) { k.islem("y_odeme_islem", govde(null, x.optInt("id"), it + ("tip" to tip))) }
            }
        }
        item { BolumBaslik("Ödeme yöntemleri") }
        items(v.optJSONArray("yontemler").nesneler(), key = { "y" + it.optInt("id") }) { m ->
            Kart(onClick = { satir(m, "yontem") }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IkonRozet(when (m.optString("tur")) { "cash" -> Icons.Filled.Payments; "card" -> Icons.Filled.CreditCard; else -> Icons.Filled.Receipt }, if (m.optBoolean("aktif")) Mavi else Soluk, 38)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(m.optString("ad"), fontWeight = FontWeight.Bold, color = if (m.optBoolean("aktif")) Murekkep else Soluk)
                        Text(m.optString("tur_ad") + if (!m.optBoolean("aktif")) " · kapalı" else "", color = Soluk, fontSize = 12.sp)
                    }
                    Rozet(oran(m.optDouble("komisyon")), if (m.optDouble("komisyon") > 0) Sari else Yesil, if (m.optDouble("komisyon") > 0) SariAcik else YesilAcik)
                }
            }
        }
        val kan = v.optJSONArray("kanallar").nesneler()
        if (kan.isNotEmpty()) {
            item { BolumBaslik("Satış kanalları (platform komisyonu)") }
            items(kan, key = { "k" + it.optInt("id") }) { c ->
                Kart(onClick = { satir(c, "kanal") }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IkonRozet(Icons.Filled.Storefront, if (c.optBoolean("aktif")) Turuncu else Soluk, 38)
                        Spacer(Modifier.width(12.dp))
                        Text(c.optString("ad"), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), color = if (c.optBoolean("aktif")) Murekkep else Soluk)
                        Rozet(oran(c.optDouble("komisyon")), if (c.optDouble("komisyon") > 0) Sari else Yesil, if (c.optDouble("komisyon") > 0) SariAcik else YesilAcik)
                    }
                }
            }
        }
        item { Text("Değiştirmek için satıra dokunun. Yeni oran bundan sonraki satışlara uygulanır.", color = Soluk, fontSize = 12.sp, modifier = Modifier.padding(4.dp)) }
    }
}

/* ======================= ŞUBELER ======================= */
@Composable
fun SubelerEkrani(geri: (() -> Unit)?) {
    VeriEkrani("Şubeler", "y_subeler", geri, altBaslik = { "${it.optJSONArray("liste")?.length() ?: 0} şube" }) { v, _ ->
        val l = v.optJSONArray("liste").nesneler()
        val topBugun = l.sumOf { it.optDouble("bugun") }; val topAy = l.sumOf { it.optDouble("ay") }
        item {
            KpiIzgara(listOf<@Composable (Modifier) -> Unit>(
                { m -> KpiKutu("Bugün toplam", tl(topBugun), Icons.Filled.Store, Turuncu, m, koyu = true) },
                { m -> KpiKutu("Bu ay toplam", tl(topAy), Icons.Filled.AccountBalance, Mavi, m) },
            ))
        }
        items(l, key = { it.optString("kod") }) { s ->
            Kart {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IkonRozet(Icons.Filled.Store, if (s.has("hata")) Kirmizi else Turuncu, 42)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(s.optString("ad"), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(if (s.has("hata")) s.optString("hata") else "${s.optInt("personel")} aktif personel", color = if (s.has("hata")) Kirmizi else Soluk, fontSize = 12.sp)
                    }
                    if (s.optBoolean("merkez")) Rozet("Merkez", Mavi, MaviAcik)
                }
                if (!s.has("hata")) {
                    Ayrac()
                    BilgiSatir("Bugün", tl(s.optDouble("bugun")) + " · ${s.optInt("adet")} adisyon", kalin = true)
                    BilgiSatir("Bu ay", tl(s.optDouble("ay")))
                    BilgiSatir("Kasa", if (s.optBoolean("kasa")) "Açık" else "Kapalı", renk = if (s.optBoolean("kasa")) Yesil else Soluk)
                }
            }
        }
        item { Text("Şubeler arası geçiş: Profil → Şube. Menü ve fiyatlar merkez şubeden yönetilir ve diğer şubelere otomatik gönderilir.", color = Soluk, fontSize = 12.sp, modifier = Modifier.padding(4.dp)) }
    }
}

/* ======================= YEMEKPOS ======================= */
@Composable
fun YemekposEkrani(geri: (() -> Unit)?) {
    VeriEkrani("YemekPOS Bağlantısı", "y_yemekpos", geri, altBaslik = { v -> if (!v.optBoolean("kurulu")) "Kurulu değil" else if (v.isNull("dk")) "" else "Son eşitleme ${v.optInt("dk")} dk önce" }) { v, _ ->
        val dk = if (v.isNull("dk")) null else v.optInt("dk")
        val saglikli = v.optBoolean("kurulu") && dk != null && dk <= 10 && v.optString("hata").isEmpty()
        item {
            Kart(koyu = true) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (saglikli) Icons.Filled.CheckCircle else Icons.Filled.Warning, null, tint = if (saglikli) YesilAcik else SariAcik)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(when { !v.optBoolean("kurulu") -> "Bağlantı kurulmamış"; saglikli -> "Bağlantı çalışıyor"; else -> "Eşitleme gecikti" }, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                        Text("Son: " + tarihSaat(v.optString("son")), fontSize = 12.sp)
                    }
                }
            }
        }
        if (v.optString("hata").isNotEmpty()) item { Serit(Icons.Filled.Error, v.optString("hata"), Kirmizi, KirmiziAcik) }
        item {
            val b = v.optJSONObject("bugun") ?: JSONObject()
            val a = v.optJSONObject("acik")
            KpiIzgara(listOf<@Composable (Modifier) -> Unit>(
                { m -> KpiKutu("Bugün gelen", tl(b.optDouble("tutar")), Icons.Filled.CloudSync, Turuncu, m, alt = "${b.optInt("adet")} adisyon") },
                { m -> KpiKutu("Açık adisyon", if (a == null) "—" else tl(a.optDouble("tutar")), Icons.Filled.Receipt, Mavi, m, alt = a?.let { "${it.optInt("adet")} adet" }) },
            ))
        }
        item {
            Kart {
                KartBaslik(Icons.Filled.Settings, "Eşitleme ayarları", renk = Mavi)
                BilgiSatir("Menü YemekPOS'tan", if (v.optBoolean("menu")) "Açık" else "Kapalı", renk = if (v.optBoolean("menu")) Yesil else Soluk)
                BilgiSatir("Satışlar stoktan düşsün", if (v.optBoolean("stok")) "Açık" else "Kapalı", renk = if (v.optBoolean("stok")) Yesil else Soluk,
                    alt = v.optString("stok_bas").ifEmpty { null }?.let { "başlangıç " + tarihSaat(it) })
                BilgiSatir("Son katalog alımı", tarihSaat(v.optString("katalog").ifEmpty { null }))
                BilgiSatir("Kurye eşleşmesi", "${v.optInt("kurye_eslesen")} / ${v.optInt("kurye_toplam")}")
            }
        }
        item { Text("YemekPOS veritabanı yalnızca okunur; buradan hiçbir şey yazılmaz. Bağlantı ayarları kasadaki bilgisayardan yapılır.", color = Soluk, fontSize = 12.sp, modifier = Modifier.padding(4.dp)) }
    }
}

/* ======================= ONLINE SİPARİŞ & ENTEGRASYON ======================= */
@Composable
fun EntegrasyonEkrani(geri: (() -> Unit)?) {
    VeriEkrani("Online Sipariş", "y_entegrasyon", geri, altBaslik = { "Son 30 gün · platformlar" }) { v, _ ->
        item {
            Kart {
                KartBaslik(Icons.Filled.Language, "Kanallar", renk = Mavi)
                BilgiSatir("Trendyol Yemek", if (v.optBoolean("trendyol")) "Bağlı" else "Bağlı değil", renk = if (v.optBoolean("trendyol")) Yesil else Soluk)
                BilgiSatir("Web'den paket sipariş", if (v.optBoolean("web_siparis")) "Açık" else "Kapalı", renk = if (v.optBoolean("web_siparis")) Yesil else Soluk)
                BilgiSatir("QR menü", if (v.optBoolean("qr_menu")) "Açık" else "Kapalı", renk = if (v.optBoolean("qr_menu")) Yesil else Soluk)
            }
        }
        val oz = v.optJSONArray("ozet").nesneler()
        if (oz.isNotEmpty()) item {
            Kart {
                KartBaslik(Icons.Filled.Storefront, "Platform özeti")
                oz.forEach { p -> BilgiSatir(p.optString("platform"), "${p.optInt("adet")} sipariş", alt = "${p.optInt("kabul")} kabul · ${p.optInt("red")} red" + if (p.optInt("yeni") > 0) " · ${p.optInt("yeni")} bekliyor" else "") }
            }
        }
        item { BolumBaslik("Son siparişler") }
        val l = v.optJSONArray("son").nesneler()
        if (l.isEmpty()) item { BosKutu("Online sipariş yok.") }
        items(l) { o ->
            Kart {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(o.optString("platform") + " · " + o.optString("musteri").ifEmpty { "Müşteri" }, fontWeight = FontWeight.Bold)
                        Text(tarihSaat(o.optString("zaman")) + (if (o.optString("adisyon").isNotEmpty()) " · adisyon " + o.optString("adisyon") else ""), color = Soluk, fontSize = 12.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(tl(o.optDouble("tutar")), fontWeight = FontWeight.Bold)
                        when (o.optString("durum_kod")) {
                            "rejected" -> Rozet(o.optString("durum"), Kirmizi, KirmiziAcik)
                            "new" -> Rozet(o.optString("durum"), Sari, SariAcik)
                            else -> Rozet(o.optString("durum"), Yesil, YesilAcik)
                        }
                    }
                }
            }
        }
    }
}

/* ======================= MUHASEBE ======================= */
@Composable
fun MuhasebeEkrani(geri: (() -> Unit)?) {
    var ay by rememberSaveable { mutableStateOf(buAy()) }
    VeriEkrani("Muhasebe Özeti", "y_muhasebe", geri, altBaslik = { it.optString("ay_ad") }, parametre = mapOf("ay" to ay), ust = { _, _ -> AySecici(ay) { ay = it } }) { v, _ ->
        val o = v.optJSONObject("ozet") ?: JSONObject()
        item {
            KpiIzgara(listOf<@Composable (Modifier) -> Unit>(
                { m -> KpiKutu("KDV dahil ciro", tl(o.optDouble("ciro")), Icons.Filled.AccountBalance, Turuncu, m, alt = "${o.optInt("adet")} adisyon", koyu = true) },
                { m -> KpiKutu("Hesaplanan KDV", tl(o.optDouble("kdv")), Icons.Filled.Receipt, Mavi, m, alt = "matrah " + tl(o.optDouble("matrah"))) },
                { m -> KpiKutu("Alış (stok girişi)", tl(o.optDouble("alim")), Icons.Filled.Store, Sari, m, alt = "indirilecek KDV " + tl(o.optDouble("alim_kdv"))) },
                { m -> KpiKutu("Giderler", tl(o.optDouble("gider")), Icons.Filled.Payments, Kirmizi, m) },
            ))
        }
        item {
            Kart {
                KartBaslik(Icons.Filled.Receipt, "Ciro kalemleri")
                BilgiSatir("Yapılan indirim", tl(o.optDouble("indirim")))
                BilgiSatir("Kart / platform komisyonu", tl(o.optDouble("komisyon")))
                BilgiSatir("KDV farkı (hesaplanan − indirilecek)", tl(o.optDouble("kdv") - o.optDouble("alim_kdv")), kalin = true)
                if (o.optInt("fatura") > 0) BilgiSatir("Fatura / e-arşiv", "${o.optInt("fatura_kesildi")} / ${o.optInt("fatura")} kesildi")
            }
        }
        val kdv = v.optJSONArray("kdv_oranlari").nesneler()
        if (kdv.isNotEmpty()) item {
            Kart {
                KartBaslik(Icons.Filled.AccountBalance, "KDV oranlarına göre", renk = Mavi)
                kdv.forEach { r -> BilgiSatir("%" + String.format(TR, "%.0f", r.optDouble("oran")) + " KDV", tl(r.optDouble("kdv")), alt = "matrah " + tl(r.optDouble("matrah"))) }
            }
        }
        val y = v.optJSONArray("yontemler").nesneler()
        if (y.isNotEmpty()) item {
            Kart {
                KartBaslik(Icons.Filled.CreditCard, "Tahsilat yöntemleri", renk = Yesil)
                y.forEach { r -> BilgiSatir(r.optString("ad"), tl(r.optDouble("tutar")), alt = if (r.optDouble("komisyon") > 0) "komisyon " + tl(r.optDouble("komisyon")) else null) }
            }
        }
        item { Text("Bu özet bilgi amaçlıdır; resmî beyan için mali müşavirinizin kayıtları esas alınır.", color = Soluk, fontSize = 12.sp, modifier = Modifier.padding(4.dp)) }
    }
}

/* ======================= AYARLAR ======================= */
@Composable
fun AyarlarEkrani(geri: (() -> Unit)?) {
    VeriEkrani("Ayarlar", "y_ayarlar", geri, altBaslik = { it.optString("dukkan").ifEmpty { it.optString("sube") } }) { v, k ->
        item {
            Kart(onClick = {
                k.ac {
                    FormPenceresi("Satış hedefleri", listOf(
                        Alan("gun", "Günlük hedef (₺)", AlanTip.SAYI, String.format(US, "%.0f", v.optDouble("hedef_gun"))),
                        Alan("ay", "Aylık hedef (₺)", AlanTip.SAYI, String.format(US, "%.0f", v.optDouble("hedef_ay"))),
                    ), kapat = k.kapat) { k.islem("y_ayar_islem", govde("hedef", null, it)) }
                }
            }) {
                KartBaslik(Icons.Filled.Flag, "Satış hedefleri", "düzenle")
                BilgiSatir("Günlük", if (v.optDouble("hedef_gun") > 0) tl(v.optDouble("hedef_gun")) else "belirlenmedi")
                BilgiSatir("Aylık", if (v.optDouble("hedef_ay") > 0) tl(v.optDouble("hedef_ay")) else "belirlenmedi")
                BilgiSatir("İş günü başlangıcı", String.format(US, "%02d:00", v.optInt("gun_baslangic", 6)))
            }
        }
        item { BolumBaslik("Özellikler") }
        items(v.optJSONArray("anahtarlar").nesneler(), key = { it.optString("ad") }) { a ->
            Kart {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(a.optString("etiket"), fontWeight = FontWeight.SemiBold)
                        Text(a.optString("aciklama"), color = Soluk, fontSize = 12.sp)
                    }
                    Switch(checked = a.optBoolean("deger"), onCheckedChange = { yeni ->
                        k.islem("y_ayar_islem", JSONObject().put("ad", a.optString("ad")).put("deger", if (yeni) "1" else ""))
                    }, colors = SwitchDefaults.colors(checkedTrackColor = Yesil))
                }
            }
        }
        item { Text("Şifreler, Telegram ve bağlantı anahtarları güvenlik nedeniyle yalnızca sunucudaki ayar dosyasından değiştirilir.", color = Soluk, fontSize = 12.sp, modifier = Modifier.padding(4.dp)) }
    }
}

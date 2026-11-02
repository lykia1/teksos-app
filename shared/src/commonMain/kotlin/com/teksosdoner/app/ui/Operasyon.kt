@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.teksosdoner.app.ui

import com.teksosdoner.app.*

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TableRestaurant
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.teksosdoner.app.gunAdi
import com.teksosdoner.app.kisaTarih
import com.teksosdoner.app.nesneler
import com.teksosdoner.app.tarihSaat
import com.teksosdoner.app.tl
import kotlinx.coroutines.delay

private fun kg(d: Double) = String.format(TR, "%.2f kg", d)

/* ======================= KURYELER ======================= */
@Composable
fun KuryelerEkrani(geri: (() -> Unit)?) {
    VeriEkrani("Kuryeler", "y_kuryeler", geri, altBaslik = { v -> "${v.optInt("bekleyen")} paket kurye bekliyor" }) { v, k ->
        val l = v.optJSONArray("liste").nesneler()
        if (v.optInt("bekleyen") > 0) item { Serit(Icons.Filled.TwoWheeler, "${v.optInt("bekleyen")} paket sipariş henüz bir kuryeye atanmadı.", Sari, SariAcik) }
        if (l.isEmpty()) item { BosKutu("Kurye olarak işaretli personel yok. Personel kartında 'Kurye' seçeneğini açın.") }
        items(l, key = { it.optInt("id") }) { c ->
            Kart {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IkonRozet(Icons.Filled.TwoWheeler, if (c.optInt("yolda") > 0) Turuncu else Mavi, 42)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(c.optString("ad"), fontWeight = FontWeight.Bold)
                        val kon = c.optJSONObject("konum")
                        Text(when {
                            kon == null -> "Konum yok"
                            kon.optInt("dk") <= 2 -> "Konum canlı"
                            kon.optInt("dk") < 60 -> "Konum ${kon.optInt("dk")} dk önce"
                            else -> "Son konum " + tarihSaat(kon.optString("zaman"))
                        }, color = if (kon != null && kon.optInt("dk") <= 5) Yesil else Soluk, fontSize = 12.sp)
                    }
                    if (c.optInt("yolda") > 0) Rozet("${c.optInt("yolda")} yolda", Turuncu, TuruncuAcik)
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniKutu("Bugün", "${c.optInt("bugun")} paket", Modifier.weight(1f))
                    MiniKutu("Ort. süre", if (c.isNull("ort_dk")) "—" else "${c.optInt("ort_dk")} dk", Modifier.weight(1f))
                    MiniKutu("Üzerindeki nakit", tl(c.optDouble("nakit")), Modifier.weight(1.3f), if (c.optDouble("nakit") > 0) Turuncu else Murekkep)
                }
                EylemSatiri(
                    *listOfNotNull<Triple<String, Color, () -> Unit>>(
                        c.optString("telefon").ifEmpty { null }?.let { tel -> Triple("Ara", Mavi) { os.telefonAra(tel) } },
                        c.optJSONObject("konum")?.let { kon -> Triple("Haritada", Mavi) { os.ac("https://www.google.com/maps/search/?api=1&query=${kon.optDouble("lat")},${kon.optDouble("lng")}") } },
                        Triple("Hesap al", Turuncu) {
                            k.ac {
                                FormPenceresi(c.optString("ad") + " · hesap", listOf(
                                    Alan("tutar", "Teslim alınan nakit (₺)", AlanTip.SAYI, String.format(US, "%.2f", c.optDouble("nakit")).replace('.', ','),
                                        ipucu = "Beklenen: " + tl(c.optDouble("nakit")) + " (" + c.optInt("paket") + " paket)"),
                                    Alan("not", "Not"),
                                ), dugme = "Teslim al", kapat = k.kapat) { k.islem("y_kurye_islem", govde(null, c.optInt("id"), it)) }
                            }
                        },
                    ).toTypedArray()
                )
            }
        }
    }
}

@Composable
fun MiniKutu(baslik: String, deger: String, modifier: Modifier = Modifier, renk: Color = Murekkep) {
    Column(modifier.background(Color(0xFFF5F5F4), RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 8.dp)) {
        Text(baslik, color = Soluk, fontSize = 11.sp, maxLines = 1)
        Text(deger, color = renk, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, maxLines = 1)
    }
}

/* ======================= DÖNER VERİMİ ======================= */
@Composable
fun DonerEkrani(geri: (() -> Unit)?) {
    VeriEkrani("Döner Verimi", "y_doner", geri, altBaslik = { "Son 30 gün · takılan / artan / kullanılan" }) { v, _ ->
        val gun = v.optJSONArray("gunler").nesneler()
        if (gun.isNotEmpty()) {
            val top = gun.sumOf { it.optDouble("kullanilan") }
            item {
                KpiIzgara(listOf<@Composable (Modifier) -> Unit>(
                    { m -> KpiKutu("30 günde kullanılan", kg(top), Icons.Filled.LocalFireDepartment, Turuncu, m) },
                    { m -> KpiKutu("Günlük ortalama", kg(top / gun.size), Icons.Filled.LocalFireDepartment, Mavi, m) },
                ))
            }
            item {
                Kart {
                    KartBaslik(Icons.Filled.LocalFireDepartment, "Günlük kullanım")
                    val son = gun.take(14).reversed()
                    CubukGrafik(son.map { it.optDouble("kullanilan") }, son.map { kisaTarih(it.optString("tarih")) }, degerYaz = false)
                }
            }
        }
        item { BolumBaslik("Şiş kayıtları") }
        val l = v.optJSONArray("kayitlar").nesneler()
        if (l.isEmpty()) item { BosKutu("Son 30 günde döner kaydı yok.") }
        items(l, key = { it.optInt("id") }) { r ->
            Kart {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(r.optString("urun").ifEmpty { "Döner" }, fontWeight = FontWeight.Bold)
                        Text(kisaTarih(r.optString("tarih")) + " " + gunAdi(r.optString("tarih")) + (if (r.optString("kisi").isNotEmpty()) " · " + r.optString("kisi") else ""), color = Soluk, fontSize = 12.sp)
                        if (r.optString("aciklama").isNotEmpty()) Text(r.optString("aciklama"), color = Soluk, fontSize = 12.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("takılan " + kg(r.optDouble("takilan")), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        if (r.isNull("artan")) Rozet("Artan girilmedi", Sari, SariAcik)
                        else Text("artan " + kg(r.optDouble("artan")) + " · kullanılan " + kg(r.optDouble("takilan") - r.optDouble("artan")), color = Soluk, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

/* ======================= MASALAR ======================= */
@Composable
fun MasalarEkrani(geri: (() -> Unit)?) {
    VeriEkrani("Masalar", "y_masalar", geri, altBaslik = { v -> v.optJSONArray("liste").nesneler().let { l -> "${l.count { !it.isNull("acik") }} dolu · ${l.count { it.optBoolean("aktif") }} aktif masa" } }) { v, k ->
        val l = v.optJSONArray("liste").nesneler()
        if (l.isEmpty()) item { BosKutu("Masa tanımlı değil.") }
        l.groupBy { it.optString("bolge").ifEmpty { "Salon" } }.forEach { (bolge, masalar) ->
            item(key = "b$bolge") { BolumBaslik(bolge) }
            item(key = "m$bolge") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp), maxItemsInEachRow = 3) {
                    masalar.forEach { t ->
                        val dolu = !t.isNull("acik")
                        Kart(Modifier.weight(1f), onClick = {
                            k.ac { OnayPenceresi(t.optString("ad"), if (t.optBoolean("aktif")) "Masa kasada gizlensin mi?" else "Masa tekrar açılsın mı?", if (t.optBoolean("aktif")) "Gizle" else "Aç", tehlikeli = false, kapat = k.kapat) {
                                k.islem("y_masa_islem", govde(null, t.optInt("id"))) } }
                        }) {
                            Icon(Icons.Filled.TableRestaurant, null, tint = when { !t.optBoolean("aktif") -> Soluk; dolu -> Turuncu; else -> Yesil })
                            Text(t.optString("ad"), fontWeight = FontWeight.Bold, color = if (t.optBoolean("aktif")) Murekkep else Soluk)
                            Text(when { !t.optBoolean("aktif") -> "kapalı"; dolu -> tl(t.optDouble("acik")) + " · ${t.optInt("dk")} dk"; else -> "${t.optInt("kisi")} kişilik · boş" },
                                color = if (dolu) Turuncu else Soluk, fontSize = 11.sp, maxLines = 1)
                        }
                    }
                    repeat((3 - masalar.size % 3) % 3) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/* ======================= İŞLEM GEÇMİŞİ ======================= */
@Composable
fun IslemlerEkrani(geri: (() -> Unit)?) {
    var aranan by rememberSaveable { mutableStateOf("") }
    var ara by remember { mutableStateOf("") }
    var sayfa by rememberSaveable { mutableIntStateOf(1) }
    LaunchedEffect(aranan) { delay(400); if (ara != aranan.trim()) { ara = aranan.trim(); sayfa = 1 } }
    VeriEkrani("İşlem Geçmişi", "y_islemler", geri, altBaslik = { "Kim, ne zaman, ne yaptı · sayfa ${it.optInt("sayfa")}" },
        parametre = mapOf("ara" to ara, "sayfa" to sayfa.toString()),
        ust = { _, _ ->
            OutlinedTextField(aranan, { aranan = it }, placeholder = { Text("Kişi, işlem veya ayrıntı ara") }, singleLine = true, leadingIcon = { Icon(Icons.Filled.Search, null) },
                trailingIcon = { if (aranan.isNotEmpty()) IconButton(onClick = { aranan = "" }) { Icon(Icons.Filled.Clear, "Temizle") } },
                shape = RoundedCornerShape(14.dp), colors = alanRenkleri(), modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 10.dp))
        }
    ) { v, _ ->
        val l = v.optJSONArray("liste").nesneler()
        if (l.isEmpty()) item { BosKutu("Kayıt yok.") }
        items(l, key = { it.optInt("id") }) { r ->
            Row(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(14.dp)).padding(12.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Filled.History, null, tint = Soluk)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Row {
                        Text(r.optString("kisi"), fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        Text(tarihSaat(r.optString("zaman")), color = Soluk, fontSize = 11.sp)
                    }
                    Text(eylemAdi(r.optString("eylem")), color = Turuncu, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    if (r.optString("detay").isNotEmpty()) Text(r.optString("detay"), fontSize = 13.sp)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { if (sayfa > 1) sayfa-- }, enabled = sayfa > 1) { Text("‹ Yeniler") }
                TextButton(onClick = { sayfa++ }, enabled = v.optBoolean("devam")) { Text("Eskiler ›") }
            }
        }
    }
}

private fun eylemAdi(e: String): String = mapOf(
    "login" to "Giriş", "logout" to "Çıkış", "product_update" to "Ürün düzenlendi", "product_create" to "Ürün eklendi", "order_cancel" to "Sipariş iptal",
    "order_void" to "Sipariş iptal", "refund" to "İade", "discount" to "İndirim", "kurye_mutabakat" to "Kurye hesabı", "odeme_ayar" to "Ödeme ayarı",
    "ayar" to "Ayar değişti", "cash_open" to "Kasa açıldı", "cash_close" to "Kasa kapandı", "item_delete" to "Ürün silindi (adisyon)", "price_change" to "Fiyat değişti",
)[e] ?: e.replace('_', ' ').replaceFirstChar { it.titlecase(TR) }

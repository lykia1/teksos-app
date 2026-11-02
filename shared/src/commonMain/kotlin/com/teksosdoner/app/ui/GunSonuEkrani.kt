@file:OptIn(ExperimentalMaterial3Api::class)

package com.teksosdoner.app.ui

import com.teksosdoner.app.*

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teksosdoner.app.Api
import com.teksosdoner.app.ApiHata
import com.teksosdoner.app.gunAdi
import com.teksosdoner.app.kisaTarih
import com.teksosdoner.app.nesneler
import com.teksosdoner.app.tarihSaat
import com.teksosdoner.app.tipAdi
import com.teksosdoner.app.tl
import kotlinx.coroutines.launch
import com.teksosdoner.app.json.JSONObject

@Composable
fun GunSonuEkrani(geri: (() -> Unit)?) {
    val kapsam = rememberCoroutineScope()
    var tarih by rememberSaveable { mutableStateOf("") }
    var veri by remember { mutableStateOf<JSONObject?>(null) }
    var hata by remember { mutableStateOf<String?>(null) }
    var yukleniyor by remember { mutableStateOf(false) }
    suspend fun yukle() {
        yukleniyor = true
        try { veri = Api.get("gunsonu", mapOf("tarih" to tarih)); hata = null } catch (e: ApiHata) { hata = e.message } finally { yukleniyor = false }
    }
    LaunchedEffect(tarih) { yukle() }
    Ekran("Gün Sonu (Z)", altBaslik = veri?.optString("tarih")?.let { kisaTarih(it) + "." + it.take(4) + " " + gunAdi(it) + " · iş günü" }, geri = geri) {
        val v = veri
        when {
            v == null && hata != null -> HataKutusu(hata!!) { kapsam.launch { yukle() } }
            v == null -> Yukleniyor()
            else -> PullToRefreshBox(isRefreshing = yukleniyor, onRefresh = { kapsam.launch { yukle() } }, modifier = Modifier.fillMaxSize()) {
                val z = v.optJSONObject("z") ?: JSONObject()
                val n = v.optJSONObject("nakit") ?: JSONObject()
                val acik = v.optJSONObject("acik") ?: JSONObject()
                LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
                    item {
                        OutlinedButton(onClick = {
                            tarihSec("Gün seçin") { tarih = it }
                        }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                            Icon(Icons.Filled.CalendarMonth, null, tint = Turuncu); Spacer(Modifier.width(8.dp)); Text("Başka gün seç", color = Murekkep, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (v.optBoolean("bugun") && acik.optInt("adet") > 0) item {
                        Serit(Icons.Filled.Warning, "Şu an ${acik.optInt("adet")} açık adisyon var (${tl(acik.optDouble("tutar"))}). Gün sonu öncesi kapatılmalı.", Sari, SariAcik)
                    }
                    item {
                        KpiIzgara(listOf<@Composable (Modifier) -> Unit>(
                            { m -> KpiKutu("Ciro (KDV dahil)", tl(z.optDouble("toplam")), Icons.Filled.Payments, Turuncu, m, koyu = true, alt = "${z.optInt("adet")} satış") },
                            { m -> KpiKutu("Kasada olması gereken nakit", tl(n.optDouble("kalan")), Icons.Filled.Money, Yesil, m, alt = "nakit satış − nakit gider") },
                        ))
                    }
                    item {
                        Kart {
                            KartBaslik(Icons.Filled.Receipt, "Z özeti")
                            BilgiSatir("Ara toplam", tl(z.optDouble("ara_toplam")))
                            BilgiSatir("İndirim", "−" + tl(z.optDouble("indirim")), renk = Kirmizi)
                            BilgiSatir("Toplam", tl(z.optDouble("toplam")), kalin = true)
                            BilgiSatir("İçindeki KDV", tl(z.optDouble("kdv")))
                            BilgiSatir("Platform / banka komisyonu", tl(z.optDouble("komisyon")))
                            BilgiSatir("İptal", "${z.optInt("iptal_adet")} · ${tl(z.optDouble("iptal_tutar"))}", renk = if (z.optInt("iptal_adet") > 0) Kirmizi else Murekkep)
                            BilgiSatir("İlk / son satış", tarihSaat(z.optString("ilk")).takeLast(5) + " – " + tarihSaat(z.optString("son")).takeLast(5))
                        }
                    }
                    val yont = v.optJSONArray("yontemler").nesneler()
                    if (yont.isNotEmpty()) item {
                        Kart {
                            KartBaslik(Icons.Filled.Payments, "Ödeme yöntemleri", renk = Mavi)
                            yont.forEach { y -> BilgiSatir(y.optString("ad"), tl(y.optDouble("tutar")), alt = "${y.optInt("adet")} sipariş" + if (y.optBoolean("nakit")) " · nakit" else "") }
                        }
                    }
                    val tip = v.optJSONArray("tipler").nesneler()
                    if (tip.isNotEmpty()) item {
                        Kart {
                            KartBaslik(Icons.Filled.Restaurant, "Satış türü")
                            tip.forEach { t -> BilgiSatir(tipAdi(t.optString("tip")), tl(t.optDouble("tutar")), alt = "${t.optInt("adet")} sipariş") }
                        }
                    }
                    item {
                        Kart {
                            KartBaslik(Icons.Filled.Money, "Nakit kasa", renk = Yesil)
                            BilgiSatir("Nakit satış", tl(n.optDouble("satis")))
                            n.optJSONArray("giderler").nesneler().forEach { g ->
                                BilgiSatir("− " + (g.optString("aciklama").ifEmpty { g.optString("kat") }), tl(g.optDouble("tutar")), renk = Kirmizi, alt = g.optString("kat"))
                            }
                            BilgiSatir("Kasada olması gereken", tl(n.optDouble("kalan")), kalin = true, renk = Yesil)
                        }
                    }
                    val ot = v.optJSONArray("oturumlar").nesneler()
                    if (ot.isNotEmpty()) item {
                        Kart {
                            KartBaslik(Icons.Filled.PointOfSale, "Kasa açılış / kapanış", renk = Mavi)
                            ot.forEach { o ->
                                Text("Açılış ${tarihSaat(o.optString("acilis"))} · ${o.optString("acan")}", fontSize = 13.sp)
                                Text(if (o.optString("kapanis").isNotEmpty() && o.optString("kapanis") != "null") "Kapanış ${tarihSaat(o.optString("kapanis"))} · ${o.optString("kapatan")}" else "Kasa hâlâ açık", color = Soluk, fontSize = 13.sp)
                                if (!o.isNull("fark")) {
                                    val f = o.optDouble("fark")
                                    BilgiSatir("Beklenen / sayılan", tl(o.optDouble("beklenen")) + " / " + tl(o.optDouble("sayilan")))
                                    BilgiSatir("Fark", (if (f > 0) "+" else "") + tl(f), renk = if (f < 0) Kirmizi else if (f > 0) Sari else Yesil, kalin = true)
                                }
                                if (o.optString("not").isNotEmpty()) Text("Not: " + o.optString("not"), color = Soluk, fontSize = 12.sp)
                                Ayrac()
                            }
                        }
                    }
                    val ur = v.optJSONArray("urunler").nesneler()
                    if (ur.isNotEmpty()) item {
                        Kart {
                            KartBaslik(Icons.Filled.Restaurant, "Satılan ürünler (adet)")
                            val enBuyuk = ur.maxOfOrNull { it.optDouble("adet") } ?: 1.0
                            ur.forEachIndexed { i, u ->
                                val adet = u.optDouble("adet").let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() }
                                SiraSatir(i + 1, u.optString("ad"), null, "$adet ad", (u.optDouble("adet") / enBuyuk).toFloat(), sagAlt = tl(u.optDouble("ciro")))
                            }
                        }
                    }
                }
            }
        }
    }
}

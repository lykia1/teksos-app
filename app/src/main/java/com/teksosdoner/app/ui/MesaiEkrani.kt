@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.teksosdoner.app.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Calendar

@Composable
fun MesaiEkrani(geri: (() -> Unit)?) {
    val ctx = LocalContext.current
    val kapsam = rememberCoroutineScope()
    var sekme by rememberSaveable { mutableStateOf("onay") }
    var tarih by rememberSaveable { mutableStateOf(bugunTarih()) }
    var veri by remember { mutableStateOf<JSONObject?>(null) }
    var hata by remember { mutableStateOf<String?>(null) }
    var yukleniyor by remember { mutableStateOf(false) }
    var mesaj by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    var silinecek by remember { mutableStateOf<JSONObject?>(null) }
    var hepsiSor by remember { mutableStateOf(false) }

    suspend fun yukle() {
        yukleniyor = true
        try { veri = Api.get("mesai", mapOf("tarih" to tarih)); hata = null } catch (e: ApiHata) { hata = e.message } finally { yukleniyor = false }
    }
    fun islem(govde: JSONObject) {
        kapsam.launch {
            try { mesaj = Api.post("mesai_islem", govde).optString("mesaj") to false } catch (e: ApiHata) { mesaj = (e.message ?: "Hata") to true }
            yukle()
        }
    }
    LaunchedEffect(tarih) { yukle() }

    Ekran("Mesai & İzinler", altBaslik = "Onaylı mesailer maaşa yansır", geri = geri, eylemler = {
        IconButton(onClick = { kapsam.launch { yukle() } }) { Icon(Icons.Filled.Refresh, "Yenile") }
    }) {
        val v = veri
        when {
            v == null && hata != null -> HataKutusu(hata!!) { kapsam.launch { yukle() } }
            v == null -> Yukleniyor()
            else -> {
                val onay = v.optJSONArray("onay").nesneler()
                val izin = v.optJSONArray("izinler").nesneler()
                val destek = v.optJSONArray("destek").nesneler()
                val gun = v.optJSONArray("gun").nesneler()
                val simdi = v.optJSONArray("simdi").nesneler()
                Column(Modifier.fillMaxSize()) {
                    LazyRow(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item { Cip2(sekme == "onay", "Onay bekleyen", onay.size) { sekme = "onay" } }
                        item { Cip2(sekme == "izin", "İzin talepleri", izin.count { it.optString("durum") == "Bekliyor" }) { sekme = "izin" } }
                        item { Cip2(sekme == "destek", "Talepler", destek.size) { sekme = "destek" } }
                        item { Cip2(sekme == "gun", "Günlük kayıtlar") { sekme = "gun" } }
                    }
                    PullToRefreshBox(isRefreshing = yukleniyor, onRefresh = { kapsam.launch { yukle() } }, modifier = Modifier.fillMaxSize()) {
                        LazyColumn(contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                            mesaj?.let { (m, h) -> item { MesajSeridi(m, h) { mesaj = null } } }
                            if (simdi.isNotEmpty()) item {
                                Kart {
                                    KartBaslik(Icons.Filled.CalendarMonth, "Şu an mesaide", "${simdi.size} kişi", renk = Yesil)
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        simdi.forEach { s ->
                                            Row(Modifier.background(YesilAcik, RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Box(Modifier.size(8.dp).background(Yesil, CircleShape)); Spacer(Modifier.width(6.dp))
                                                Text(s.optString("ad") + " · " + s.optString("giris") + "'den beri", fontSize = 13.sp, color = Yesil, fontWeight = FontWeight.SemiBold)
                                            }
                                        }
                                    }
                                }
                            }
                            when (sekme) {
                                "onay" -> {
                                    if (onay.isEmpty()) item { BosKutu("Onay bekleyen mesai yok 👍") }
                                    else item {
                                        Button(onClick = { hepsiSor = true }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Yesil)) {
                                            Icon(Icons.Filled.DoneAll, null); Spacer(Modifier.width(8.dp)); Text("Dünden öncekilerin hepsini onayla", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    items(onay, key = { "o" + it.optInt("id") }) { m -> MesaiKart(m, tarihGoster = true, islem = { islem(it) }, sil = { silinecek = m }) }
                                }
                                "izin" -> {
                                    if (izin.isEmpty()) item { BosKutu("İzin talebi yok.") }
                                    items(izin, key = { "i" + it.optInt("id") }) { i -> IzinKart(i) { durum -> islem(JSONObject().put("act", "izin").put("id", i.optInt("id")).put("durum", durum)) } }
                                }
                                "destek" -> {
                                    if (destek.isEmpty()) item { BosKutu("Bekleyen düzeltme / eksik ürün talebi yok.") }
                                    items(destek, key = { "d" + it.optInt("id") }) { d ->
                                        Kart {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                IkonRozet(Icons.Filled.ReportProblem, Sari, 36); Spacer(Modifier.width(10.dp))
                                                Column(Modifier.weight(1f)) {
                                                    Text(d.optString("konu"), fontWeight = FontWeight.Bold)
                                                    Text(d.optString("ad") + " · " + tarihSaat(d.optString("zaman")), color = Soluk, fontSize = 12.sp)
                                                }
                                            }
                                            if (d.optString("mesaj").isNotEmpty()) Text(d.optString("mesaj"), fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
                                            TextButton(onClick = { islem(JSONObject().put("act", "destek").put("id", d.optInt("id"))) }, modifier = Modifier.align(Alignment.End)) {
                                                Text("Çözüldü olarak işaretle", color = Yesil, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                                else -> {
                                    item {
                                        OutlinedButton(onClick = {
                                            val c = Calendar.getInstance()
                                            DatePickerDialog(ctx, { _, y, m, d -> tarih = String.format(java.util.Locale.US, "%04d-%02d-%02d", y, m + 1, d) },
                                                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                                        }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                                            Icon(Icons.Filled.CalendarMonth, null, tint = Turuncu); Spacer(Modifier.width(8.dp))
                                            Text(kisaTarih(tarih) + "." + tarih.take(4) + " " + gunAdi(tarih), color = Murekkep, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    val toplam = gun.sumOf { if (it.isNull("saat")) 0.0 else it.optDouble("saat") }
                                    item { Text("${gun.size} kayıt · toplam ${String.format(TR, "%.1f", toplam)} saat", color = Soluk, fontSize = 13.sp, modifier = Modifier.padding(start = 4.dp)) }
                                    if (gun.isEmpty()) item { BosKutu("Bu gün için kayıt yok.") }
                                    items(gun, key = { "g" + it.optInt("id") }) { m -> MesaiKart(m, tarihGoster = false, islem = { islem(it) }, sil = { silinecek = m }) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    silinecek?.let { m ->
        OnayPenceresi("Kayıt silinsin mi?", "${m.optString("ad")} · ${kisaTarih(m.optString("tarih"))} mesai kaydı kalıcı olarak silinecek.", "Sil", kapat = { silinecek = null }) {
            islem(JSONObject().put("act", "sil").put("id", m.optInt("id")))
        }
    }
    if (hepsiSor) OnayPenceresi("Hepsi onaylansın mı?", "Bugünden önceki, çıkışı yapılmış tüm mesailer onaylanacak ve maaşa yansıyacak.", "Onayla", tehlikeli = false, kapat = { hepsiSor = false }) {
        islem(JSONObject().put("act", "hepsini_onayla"))
    }
}

@Composable
private fun MesaiKart(m: JSONObject, tarihGoster: Boolean, islem: (JSONObject) -> Unit, sil: () -> Unit) {
    val normal = m.optString("tur") == "normal"
    val devam = m.optBoolean("devam")
    val onayli = m.optBoolean("onayli")
    Kart {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).background(TuruncuAcik, CircleShape), contentAlignment = Alignment.Center) {
                Text(m.optString("ad").take(1), color = Turuncu, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(m.optString("ad"), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(
                    (if (tarihGoster) kisaTarih(m.optString("tarih")) + " " + gunAdi(m.optString("tarih")) + " · " else "") +
                        (if (normal) m.optString("giris") + " → " + (if (devam) "devam ediyor" else m.optString("cikis")) else m.optString("tur_ad")),
                    color = Soluk, fontSize = 13.sp
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(if (m.isNull("saat")) "—" else String.format(TR, "%.1f sa", m.optDouble("saat")), fontWeight = FontWeight.ExtraBold)
                when {
                    onayli -> Rozet("Onaylı", Yesil, YesilAcik)
                    devam -> Rozet("Açık", Mavi, MaviAcik)
                    else -> Rozet("Bekliyor", Sari, SariAcik)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = sil) { Text("Sil", color = Kirmizi) }
            if (onayli) TextButton(onClick = { islem(JSONObject().put("act", "geri_al").put("id", m.optInt("id"))) }) { Text("Onayı geri al", color = Soluk) }
            else if (!devam) Button(onClick = { islem(JSONObject().put("act", "onayla").put("id", m.optInt("id"))) }, colors = ButtonDefaults.buttonColors(containerColor = Yesil), shape = RoundedCornerShape(10.dp)) {
                Text("Onayla", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun IzinKart(i: JSONObject, karar: (String) -> Unit) {
    val durum = i.optString("durum")
    Kart {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IkonRozet(Icons.Filled.EventBusy, Mavi, 38); Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(i.optString("ad"), fontWeight = FontWeight.Bold)
                Text(kisaTarih(i.optString("bas")) + " " + gunAdi(i.optString("bas")) + " – " + kisaTarih(i.optString("bit")) + " " + gunAdi(i.optString("bit")), color = Soluk, fontSize = 13.sp)
            }
            when (durum) {
                "Onaylandi" -> Rozet("Onaylandı", Yesil, YesilAcik)
                "Reddedildi" -> Rozet("Reddedildi", Kirmizi, KirmiziAcik)
                else -> Rozet("Bekliyor", Sari, SariAcik)
            }
        }
        if (i.optString("aciklama").isNotEmpty()) Text(i.optString("aciklama"), fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
        if (durum == "Bekliyor") {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { karar("Reddedildi") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) { Text("Reddet", color = Kirmizi) }
                Button(onClick = { karar("Onaylandi") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = Yesil)) { Text("Onayla", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

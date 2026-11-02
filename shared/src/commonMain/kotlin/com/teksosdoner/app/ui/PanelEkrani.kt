@file:OptIn(ExperimentalMaterial3Api::class)

package com.teksosdoner.app.ui

import com.teksosdoner.app.*

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teksosdoner.app.Api
import com.teksosdoner.app.ApiHata
import com.teksosdoner.app.TR
import com.teksosdoner.app.gunAdi
import com.teksosdoner.app.kisaTarih
import com.teksosdoner.app.nesneler
import com.teksosdoner.app.tarihSaat
import com.teksosdoner.app.tl
import kotlinx.coroutines.launch
import com.teksosdoner.app.json.JSONObject

@Composable
fun PanelEkrani(geri: (() -> Unit)?, mesaiAc: () -> Unit) {
    val kapsam = rememberCoroutineScope()
    var veri by remember { mutableStateOf<JSONObject?>(null) }
    var hata by remember { mutableStateOf<String?>(null) }
    var yukleniyor by remember { mutableStateOf(false) }
    var mesaj by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    var pencere by remember { mutableStateOf("") }
    suspend fun yukle() {
        yukleniyor = true
        try { veri = Api.get("panel"); hata = null } catch (e: ApiHata) { hata = e.message } finally { yukleniyor = false }
    }
    fun gonder(govde: JSONObject) {
        pencere = ""
        kapsam.launch {
            try { mesaj = Api.post("panel_islem", govde).optString("mesaj") to false } catch (e: ApiHata) { mesaj = (e.message ?: "Hata") to true }
            yukle()
        }
    }
    LaunchedEffect(Unit) { yukle() }

    Ekran("Panelim", altBaslik = "Mesai, vardiya, maaş ve talepler", geri = geri) {
        val v = veri
        when {
            v == null && hata != null -> HataKutusu(hata!!) { kapsam.launch { yukle() } }
            v == null -> Yukleniyor()
            else -> PullToRefreshBox(isRefreshing = yukleniyor, onRefresh = { kapsam.launch { yukle() } }, modifier = Modifier.fillMaxSize()) {
                val aktif = v.optJSONObject("aktif")
                val hk = v.optJSONObject("hakedis") ?: JSONObject()
                LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
                    mesaj?.let { (m, h) -> item { MesajSeridi(m, h) { mesaj = null } } }
                    item {
                        Kart(koyu = true) {
                            if (aktif != null) {
                                val dk = aktif.optInt("dk")
                                Text("ŞU AN MESAİDESİNİZ", color = Color(0xFF86EFAC), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                                Text("${dk / 60} sa ${dk % 60} dk", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
                                Text("Giriş ${aktif.optString("giris")} · çıkışta QR kodu tekrar okutun", color = Color(0xFFD6D3D1), fontSize = 13.sp)
                            } else {
                                Text("ŞU AN MESAİDE DEĞİLSİNİZ", color = Color(0xFFA8A29E), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                                Text("Dükkândaki QR kodu okutun", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(vertical = 4.dp))
                                Text("Konum ve yüz fotoğrafı doğrulanır.", color = Color(0xFFD6D3D1), fontSize = 13.sp)
                            }
                            Spacer(Modifier.height(14.dp))
                            Button(onClick = mesaiAc, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = if (aktif != null) Kirmizi else Turuncu)) {
                                Icon(Icons.Filled.QrCodeScanner, null); Spacer(Modifier.width(10.dp))
                                Text(if (aktif != null) "Mesaiyi bitir (QR)" else "Mesaiye başla (QR)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                    item {
                        Kart {
                            KartBaslik(Icons.Filled.CalendarMonth, "Bu haftaki vardiyam", renk = Mavi)
                            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
                                v.optJSONArray("hafta").nesneler().forEach { g ->
                                    val izinli = g.optString("yer") == "İzinli" || g.optString("tip").contains("İzin", true)
                                    val bos = g.optString("tip").isEmpty()
                                    Column(
                                        Modifier.weight(1f).background(
                                            when { g.optBoolean("bugun") -> TuruncuAcik; izinli -> MaviAcik; else -> Color(0xFFF5F5F4) }, RoundedCornerShape(10.dp)
                                        ).padding(vertical = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(gunAdi(g.optString("tarih")), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (g.optBoolean("bugun")) Turuncu else Murekkep)
                                        Text(kisaTarih(g.optString("tarih")), fontSize = 10.sp, color = Soluk)
                                        Spacer(Modifier.height(4.dp))
                                        Text(if (bos) "—" else if (izinli) "İzin" else g.optString("tip"), fontSize = 10.sp, textAlign = TextAlign.Center, maxLines = 2,
                                            color = if (izinli) Mavi else Murekkep, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Kart {
                            KartBaslik(Icons.Filled.Wallet, "Maaşım · " + hk.optString("ay"), renk = Yesil)
                            Text(tl(hk.optDouble("net")), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Yesil)
                            Text("şu ana kadarki net hak ediş (avans düşülmüş)", color = Soluk, fontSize = 12.sp)
                            Ayrac()
                            if (hk.optString("tip") == "saatlik") {
                                BilgiSatir("Onaylı mesai", String.format(TR, "%.1f saat × %s", hk.optDouble("onayli_saat"), tl(hk.optDouble("ucret"))))
                                if (hk.optDouble("bekleyen_saat") > 0) BilgiSatir("Onay bekleyen", String.format(TR, "%.1f saat (%s)", hk.optDouble("bekleyen_saat"), tl(hk.optDouble("bekleyen_tutar"))), renk = Sari)
                            } else BilgiSatir("Aylık ücret", tl(hk.optDouble("ucret")))
                            BilgiSatir("Brüt hak ediş", tl(hk.optDouble("brut")))
                            BilgiSatir("Alınan avans", "−" + tl(hk.optDouble("avans")), renk = Kirmizi)
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            IslemKutu("İzin iste", Icons.Filled.EventAvailable, Mavi, Modifier.weight(1f)) { pencere = "izin" }
                            IslemKutu("Eksik ürün", Icons.Filled.ReportProblem, Kirmizi, Modifier.weight(1f)) { pencere = "eksik" }
                            IslemKutu("Şifrem", Icons.Filled.Key, Soluk, Modifier.weight(1f)) { pencere = "sifre" }
                        }
                    }
                    val izinler = v.optJSONArray("izinler").nesneler()
                    if (izinler.isNotEmpty()) item {
                        Kart {
                            KartBaslik(Icons.Filled.EventAvailable, "İzin taleplerim", renk = Mavi)
                            izinler.forEach { i ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(kisaTarih(i.optString("bas")) + " – " + kisaTarih(i.optString("bit")), fontWeight = FontWeight.SemiBold)
                                        Text(i.optString("aciklama"), color = Soluk, fontSize = 12.sp, maxLines = 1)
                                    }
                                    when (i.optString("durum")) {
                                        "Onaylandi" -> Rozet("Onaylandı", Yesil, YesilAcik)
                                        "Reddedildi" -> Rozet("Reddedildi", Kirmizi, KirmiziAcik)
                                        else -> Rozet("Bekliyor", Sari, SariAcik)
                                    }
                                }
                            }
                        }
                    }
                    val mes = v.optJSONArray("mesailer").nesneler()
                    if (mes.isNotEmpty()) item {
                        Kart {
                            KartBaslik(Icons.Filled.History, "Son mesailerim")
                            mes.forEach { m ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(kisaTarih(m.optString("tarih")) + " " + gunAdi(m.optString("tarih")), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        Text(if (m.optBoolean("normal")) m.optString("giris") + " → " + m.optString("cikis").ifEmpty { "devam" } else m.optString("tur"), color = Soluk, fontSize = 12.sp)
                                    }
                                    Text(if (m.isNull("saat")) "—" else String.format(TR, "%.1f sa", m.optDouble("saat")), fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp))
                                    if (m.optBoolean("onayli")) Rozet("Onaylı", Yesil, YesilAcik) else Rozet("Bekliyor", Sari, SariAcik)
                                }
                            }
                        }
                    }
                    val duy = v.optJSONArray("duyurular").nesneler()
                    if (duy.isNotEmpty()) item {
                        Kart {
                            KartBaslik(Icons.Filled.Campaign, "Duyurular", renk = Color(0xFF7C3AED))
                            duy.forEach { d ->
                                Text(d.optString("baslik").ifEmpty { "Duyuru" }, fontWeight = FontWeight.Bold)
                                Text(d.optString("metin"), fontSize = 14.sp)
                                Text(tarihSaat(d.optString("tarih")), color = Soluk, fontSize = 11.sp)
                                Ayrac()
                            }
                        }
                    }
                }
            }
        }
    }

    when (pencere) {
        "izin" -> {
            var bas by remember { mutableStateOf(bugunTarih()) }
            var bit by remember { mutableStateOf(bugunTarih()) }
            var acik by remember { mutableStateOf("") }
            fun sec(ilk: Boolean) {
                tarihSec(if (ilk) "İlk gün" else "Son gün") { t -> if (ilk) { bas = t; if (bit < t) bit = t } else bit = t }
            }
            AlertDialog(onDismissRequest = { pencere = "" }, title = { Text("İzin talebi", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { sec(true) }, modifier = Modifier.weight(1f)) { Text("Başlangıç\n" + kisaTarih(bas), color = Murekkep, textAlign = TextAlign.Center) }
                            OutlinedButton(onClick = { sec(false) }, modifier = Modifier.weight(1f)) { Text("Bitiş\n" + kisaTarih(bit), color = Murekkep, textAlign = TextAlign.Center) }
                        }
                        OutlinedTextField(acik, { acik = it.take(500) }, label = { Text("Açıklama (neden)") }, colors = alanRenkleri(), modifier = Modifier.fillMaxWidth())
                    }
                },
                confirmButton = { Button(onClick = { gonder(JSONObject().put("act", "izin").put("bas", bas).put("bit", bit).put("aciklama", acik)) }, enabled = acik.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Turuncu)) { Text("Gönder") } },
                dismissButton = { TextButton(onClick = { pencere = "" }) { Text("Vazgeç", color = Soluk) } })
        }
        "eksik" -> {
            var urun by remember { mutableStateOf("") }
            var aci by remember { mutableStateOf("az") }
            var not by remember { mutableStateOf("") }
            AlertDialog(onDismissRequest = { pencere = "" }, title = { Text("Eksik ürün bildir", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SecimKutusu("Ürün", veri?.optJSONArray("stok").nesneler().map { it.optInt("id").toString() to it.optString("ad") }, urun) { urun = it }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Cip2(aci == "az", "Az kaldı") { aci = "az" }
                            Cip2(aci == "bitti", "Tükendi") { aci = "bitti" }
                        }
                        OutlinedTextField(not, { not = it.take(300) }, label = { Text("Not (isteğe bağlı)") }, colors = alanRenkleri(), modifier = Modifier.fillMaxWidth())
                    }
                },
                confirmButton = { Button(onClick = { gonder(JSONObject().put("act", "eksik").put("urun_id", urun.toIntOrNull() ?: 0).put("aciliyet", aci).put("aciklama", not)) },
                    enabled = urun.isNotEmpty(), colors = ButtonDefaults.buttonColors(containerColor = Kirmizi)) { Text("Bildir") } },
                dismissButton = { TextButton(onClick = { pencere = "" }) { Text("Vazgeç", color = Soluk) } })
        }
        "sifre" -> {
            var eski by remember { mutableStateOf("") }
            var yeni by remember { mutableStateOf("") }
            var yeni2 by remember { mutableStateOf("") }
            AlertDialog(onDismissRequest = { pencere = "" }, title = { Text("Şifre değiştir", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(eski, { eski = it }, label = { Text("Mevcut şifre") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), colors = alanRenkleri())
                        OutlinedTextField(yeni, { yeni = it }, label = { Text("Yeni şifre (en az 4)") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), colors = alanRenkleri())
                        OutlinedTextField(yeni2, { yeni2 = it }, label = { Text("Yeni şifre (tekrar)") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), colors = alanRenkleri(),
                            isError = yeni2.isNotEmpty() && yeni2 != yeni)
                    }
                },
                confirmButton = { Button(onClick = { gonder(JSONObject().put("act", "sifre").put("eski", eski).put("yeni", yeni)) }, enabled = eski.isNotEmpty() && yeni.length >= 4 && yeni == yeni2,
                    colors = ButtonDefaults.buttonColors(containerColor = Turuncu)) { Text("Kaydet") } },
                dismissButton = { TextButton(onClick = { pencere = "" }) { Text("Vazgeç", color = Soluk) } })
        }
    }
}

@Composable
private fun IslemKutu(ad: String, ikon: androidx.compose.ui.graphics.vector.ImageVector, renk: Color, modifier: Modifier, tikla: () -> Unit) {
    Kart(modifier = modifier, onClick = tikla) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            IkonRozet(ikon, renk, 40)
            Spacer(Modifier.height(6.dp))
            Text(ad, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }
}

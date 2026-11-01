@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.teksosdoner.app.ui

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teksosdoner.app.Api
import com.teksosdoner.app.ApiHata
import com.teksosdoner.app.gunAdi
import com.teksosdoner.app.kisaTarih
import com.teksosdoner.app.kisaTl
import com.teksosdoner.app.nesneler
import com.teksosdoner.app.tl
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun PatronEkrani() {
    var veri by remember { mutableStateOf<JSONObject?>(null) }
    var hata by remember { mutableStateOf<String?>(null) }
    var yenileniyor by remember { mutableStateOf(false) }
    val kapsam = rememberCoroutineScope()

    suspend fun yukle() {
        try {
            veri = Api.get("patron")
            hata = null
        } catch (e: ApiHata) {
            hata = e.message
        }
    }
    fun yenile() {
        kapsam.launch { yenileniyor = true; yukle(); yenileniyor = false }
    }
    LaunchedEffect(Unit) {
        while (true) {
            yukle()
            delay(60_000)
        }
    }

    Ekran("Özet", altBaslik = veri?.let { "Anlık durum · " + it.optString("zaman") + " · dakikada bir yenilenir" }, eylemler = {
        IconButton(onClick = { yenile() }) { Icon(Icons.Filled.Refresh, contentDescription = "Yenile") }
    }) {
        val v = veri
        when {
            v == null && hata != null -> HataKutusu(hata!!) { yenile() }
            v == null -> Yukleniyor()
            else -> PullToRefreshBox(isRefreshing = yenileniyor, onRefresh = { yenile() }, modifier = Modifier.fillMaxSize()) {
                val subeler = v.optJSONArray("subeler").nesneler()
                LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
                    if (hata != null) item { Rozet("⚠ " + hata!!, Kirmizi, KirmiziAcik) }
                    if (subeler.size > 1) item { ToplamKart(subeler) }
                    items(subeler) { SubeKart(it) }
                    item { Spacer(Modifier.height(8.dp)) }
                }
            }
        }
    }
}

@Composable
private fun ToplamKart(subeler: List<JSONObject>) {
    val ciro = subeler.sumOf { it.optJSONObject("bugun")?.optDouble("ciro") ?: 0.0 }
    val adet = subeler.sumOf { it.optJSONObject("bugun")?.optInt("adet") ?: 0 }
    val gh = subeler.sumOf { it.optDouble("gecen_hafta", 0.0) }
    val acik = subeler.sumOf { it.optJSONObject("acik")?.optDouble("tutar") ?: 0.0 } +
        subeler.sumOf { it.optJSONObject("yp_acik")?.optDouble("tutar") ?: 0.0 }
    Kart(koyu = true) {
        Text("TÜM ŞUBELER · BUGÜN", color = Color(0xFFD6D3D1), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(tl(ciro), fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Spacer(Modifier.width(10.dp))
            DegisimRozet(ciro, gh)
        }
        Text("$adet satış · geçen hafta bu saate: ${tl(gh)}", color = Color(0xFFD6D3D1), fontSize = 13.sp)
        if (acik > 0) Text("Açık adisyonlar: ${tl(acik)} (kapanınca ciroya eklenir)", color = Color(0xFFFDBA74), fontSize = 13.sp)
        Spacer(Modifier.height(10.dp))
        subeler.forEach { s ->
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                Text(s.optString("ad"), Modifier.weight(1f), color = Color(0xFFE7E5E4), fontSize = 14.sp)
                Text(tl(s.optJSONObject("bugun")?.optDouble("ciro") ?: 0.0), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun SubeKart(s: JSONObject) {
    Kart {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🏪 " + s.optString("ad"), Modifier.weight(1f), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            if (s.has("gun")) Text("İş günü " + kisaTarih(s.optString("gun")), fontSize = 12.sp, color = Soluk)
        }
        if (s.has("hata")) {
            Spacer(Modifier.height(8.dp))
            Text(s.optString("hata"), color = Kirmizi)
            return@Kart
        }
        val bugun = s.getJSONObject("bugun")
        val ciro = bugun.optDouble("ciro")
        val gh = s.optDouble("gecen_hafta")
        Spacer(Modifier.height(10.dp))
        Text("Bugün ciro", color = Soluk, fontSize = 13.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(tl(ciro), fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.width(10.dp))
            DegisimRozet(ciro, gh)
        }
        Text("${bugun.optInt("adet")} satış · ort. sepet ${tl(bugun.optDouble("sepet"))}", color = Soluk, fontSize = 13.sp)
        Text("Geçen hafta bu saate: ${tl(gh)} · dün (tam gün): ${tl(s.optDouble("dun"))}", color = Soluk, fontSize = 13.sp)

        val hedef = s.optDouble("hedef_gun")
        if (hedef > 0) {
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { (ciro / hedef).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp), color = Turuncu, trackColor = Color(0xFFF1EDE7)
            )
            Text("Günlük hedef ${tl(hedef)} · %${(ciro / hedef * 100).toInt()}", fontSize = 12.sp, color = Soluk)
        }

        // Durum rozetleri
        Spacer(Modifier.height(10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Rozet(if (s.optBoolean("kasa_acik")) "● Kasa açık" else "○ Kasa kapalı", if (s.optBoolean("kasa_acik")) Yesil else Soluk, if (s.optBoolean("kasa_acik")) YesilAcik else Color(0xFFF5F5F4))
            val acik = s.optJSONObject("acik")
            if (acik != null && acik.optInt("adet") > 0) Rozet("Açık adisyon ${acik.optInt("adet")} · ${tl(acik.optDouble("tutar"))}", Sari, SariAcik)
            val yp = s.optJSONObject("yp_acik")
            if (yp != null && yp.optInt("adet") > 0) Rozet("YemekPOS açık ${yp.optInt("adet")} · ${tl(yp.optDouble("tutar"))}", Sari, SariAcik)
            val ypd = s.optJSONObject("yp_durum")
            if (ypd != null) {
                if (ypd.optBoolean("sorun")) Rozet("● YemekPOS aksama · son ${ypd.optString("saat")}", Kirmizi, KirmiziAcik)
                else Rozet("● YemekPOS aktarımı çalışıyor", Yesil, YesilAcik)
            }
            if (s.optInt("online_bekleyen") > 0) Rozet("🔔 Online bekleyen ${s.optInt("online_bekleyen")}", Mavi, MaviAcik)
            if (s.optInt("kritik_stok") > 0) Rozet("📦 Kritik stok ${s.optInt("kritik_stok")}", Kirmizi, KirmiziAcik)
        }

        Ayrac()
        val net = s.optDouble("net")
        BilgiSatir("Tahmini net kâr", tl(net), renk = if (net < 0) Kirmizi else Yesil, kalin = true, alt = "KDV, maliyet, komisyon ve gider sonrası")
        Text(
            "Maliyet ${tl(s.optDouble("maliyet"))} · komisyon ${tl(s.optDouble("komisyon"))} · gider ${tl(s.optDouble("gider"))}" +
                (if (s.optInt("maliyetsiz_kalem") > 0) " · ${s.optInt("maliyetsiz_kalem")} kalemin maliyeti yok" else ""),
            fontSize = 12.sp, color = Soluk
        )
        Ayrac()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Bu ay (${s.optInt("ay_gecen_gun")}/${s.optInt("ay_gun")} gün)", Modifier.weight(1f), color = Soluk, fontSize = 14.sp)
            DegisimRozet(s.optDouble("ay"), s.optDouble("gecen_ay"))
            Spacer(Modifier.width(6.dp))
            Text(tl(s.optDouble("ay")), fontWeight = FontWeight.Bold)
        }
        BilgiSatir("Ay sonu tahmini", tl(s.optDouble("ay_tahmin")), alt = "Geçen ay toplam ${tl(s.optDouble("gecen_ay_tam"))}")
        val hedefAy = s.optDouble("hedef_ay")
        if (hedefAy > 0) BilgiSatir("Aylık hedef", "${tl(hedefAy)} · %${(s.optDouble("ay") / hedefAy * 100).toInt()}")

        // Son 7 gün
        val son7 = s.optJSONArray("son7").nesneler()
        if (son7.isNotEmpty()) {
            Ayrac()
            BolumBaslik("Son 7 gün")
            Cubuklar(
                son7.map { it.optDouble("t") }, son7.map { gunAdi(it.optString("gun")) },
                ustYazi = son7.map { kisaTl(it.optDouble("t")) }
            )
        }
        // Saatlik (bugün / geçen hafta aynı gün)
        val saatlik = s.optJSONArray("saatlik").nesneler()
        val dolu = saatlik.indices.filter { saatlik[it].optDouble("t") > 0 || saatlik[it].optDouble("gh") > 0 }
        if (dolu.isNotEmpty()) {
            val dilim = saatlik.subList(dolu.first(), dolu.last() + 1)
            Ayrac()
            BolumBaslik("Saatlik satış (gri: geçen hafta)")
            Cubuklar(
                dilim.map { it.optDouble("t") }, dilim.mapIndexed { i, x -> if (i % 2 == 0) x.optInt("saat").toString() else "" },
                karsi = dilim.map { it.optDouble("gh") }, yukseklik = 90.dp, vurgu = -1
            )
        }
        val kanal = s.optJSONArray("kanal").nesneler()
        if (kanal.isNotEmpty()) {
            Ayrac()
            BolumBaslik("Satış kanalları")
            DagilimListe(kanal.map { Triple(it.optString("ad"), it.optDouble("t"), "(${it.optInt("adet")})") })
        }
        val odeme = s.optJSONArray("odeme").nesneler()
        if (odeme.isNotEmpty()) {
            Ayrac()
            BolumBaslik("Ödemeler")
            DagilimListe(odeme.map { Triple(it.optString("ad"), it.optDouble("t"), null) })
        }
        val enCok = s.optJSONArray("en_cok").nesneler()
        if (enCok.isNotEmpty()) {
            Ayrac()
            BolumBaslik("En çok satanlar")
            enCok.forEachIndexed { i, u ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(26.dp)) { Text("${i + 1}.", color = Turuncu, fontWeight = FontWeight.Bold) }
                    Text(u.optString("ad"), Modifier.weight(1f), fontSize = 14.sp, maxLines = 1)
                    Text("${u.optDouble("adet").let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() }} ad · ", color = Soluk, fontSize = 13.sp)
                    Text(tl(u.optDouble("t")), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }
        }
        val mesaide = s.optJSONArray("mesaide").nesneler()
        Ayrac()
        BolumBaslik("Şu an mesaide (${mesaide.size})")
        if (mesaide.isEmpty()) Text("Kimse giriş yapmadı", color = Soluk, fontSize = 14.sp)
        else FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            mesaide.forEach { m ->
                Text(
                    m.optString("ad") + " · " + m.optString("giris"), fontSize = 13.sp,
                    modifier = Modifier.background(Color(0xFFF5F5F4), RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

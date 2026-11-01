@file:OptIn(ExperimentalMaterial3Api::class)

package com.teksosdoner.app.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teksosdoner.app.Api
import com.teksosdoner.app.ApiHata
import com.teksosdoner.app.Konum
import com.teksosdoner.app.Oturum
import com.teksosdoner.app.nesneler
import com.teksosdoner.app.tl
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun KuryeEkrani() {
    val ctx = LocalContext.current
    val kapsam = rememberCoroutineScope()
    val ben by Oturum.ben.collectAsState()
    var veri by remember { mutableStateOf<JSONObject?>(null) }
    var hata by remember { mutableStateOf<String?>(null) }
    var yenileniyor by remember { mutableStateOf(false) }
    var islemde by remember { mutableStateOf<Int?>(null) }
    var mesaj by remember { mutableStateOf<String?>(null) }
    var teslimPaket by remember { mutableStateOf<JSONObject?>(null) }
    var konumAcik by remember { mutableStateOf(Konum.calisiyor()) }
    var oncekiBekleyen by remember { mutableStateOf<Set<Int>?>(null) }

    val konumBilgi = ben?.optJSONObject("kurye_konum")
    fun konumBaslat() {
        val url = konumBilgi?.optString("url").orEmpty()
        val kod = konumBilgi?.optString("kod").orEmpty()
        if (url.isEmpty() || kod.isEmpty()) { mesaj = "Konum kodu alınamadı. Siteyi güncelleyin."; return }
        konumAcik = Konum.baslat(ctx, url, kod)
        if (konumAcik) {
            Oturum.konumKapatildi = 0
            if (!Oturum.pilSoruldu) { Oturum.pilSoruldu = true; Konum.pilIzniIste(ctx) }
        }
    }
    val izinIste = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { s ->
        if (s[Manifest.permission.ACCESS_FINE_LOCATION] == true || s[Manifest.permission.ACCESS_COARSE_LOCATION] == true) konumBaslat()
        else mesaj = "Konum izni verilmedi. Ayarlar → Uygulamalar → Teksos → İzinler → Konum: İzin ver."
    }
    fun konumAc() {
        if (Konum.izinVar(ctx)) konumBaslat()
        else {
            val izinler = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= 33) izinler += Manifest.permission.POST_NOTIFICATIONS
            izinIste.launch(izinler.toTypedArray())
        }
    }
    // Ekran açılınca konum paylaşımı kendiliğinden başlar (kurye son 8 saatte kapatmadıysa)
    LaunchedEffect(konumBilgi?.optString("kod")) {
        if (konumBilgi != null && !Konum.calisiyor() && System.currentTimeMillis() - Oturum.konumKapatildi > 8 * 3600_000L) konumAc()
    }

    suspend fun yukle() {
        try {
            val j = Api.get("kurye")
            val yeni = j.optJSONArray("bekleyen").nesneler().map { it.optInt("id") }.toSet()
            val once = oncekiBekleyen
            if (once != null && (yeni - once).isNotEmpty()) {
                runCatching {
                    val v = ctx.getSystemService(Vibrator::class.java)
                    v?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 250, 120, 250), -1))
                }
            }
            oncekiBekleyen = yeni
            veri = j
            hata = null
        } catch (e: ApiHata) {
            hata = e.message
        }
    }
    fun yenile() = kapsam.launch { yenileniyor = true; yukle(); yenileniyor = false }
    LaunchedEffect(Unit) {
        while (true) {
            yukle()
            konumAcik = Konum.calisiyor()
            delay(20_000)
        }
    }
    fun islem(id: Int, act: String, odeme: String = "nakit") {
        kapsam.launch {
            islemde = id
            try {
                Api.post("kurye_islem", JSONObject().put("id", id).put("act", act).put("odeme", odeme))
                mesaj = when (act) { "al" -> "Paket sizde, iyi yolculuklar 🛵"; "teslim" -> "Teslim edildi ✓"; else -> "Paket bırakıldı" }
                yukle()
            } catch (e: ApiHata) {
                mesaj = e.message
                yukle()
            } finally {
                islemde = null
            }
        }
    }

    Ekran("Kurye · " + (ben?.optString("ad")?.substringBefore(' ') ?: ""), altBaslik = ben?.optString("sube_ad"), eylemler = {
        IconButton(onClick = { yenile() }) { Icon(Icons.Filled.Refresh, "Yenile") }
    }) {
        val v = veri
        when {
            v == null && hata != null -> HataKutusu(hata!!) { yenile() }
            v == null -> Yukleniyor()
            else -> PullToRefreshBox(isRefreshing = yenileniyor, onRefresh = { yenile() }, modifier = Modifier.fillMaxSize()) {
                val bende = v.optJSONArray("bende").nesneler()
                val bekleyen = v.optJSONArray("bekleyen").nesneler()
                val bugun = v.optJSONArray("bugun").nesneler()
                val ust = v.optJSONObject("ustumde") ?: JSONObject()
                LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
                    item {
                        // Konum paylaşımı
                        Kart {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(12.dp).background(if (konumAcik) Yesil else Color(0xFFA8A29E), CircleShape))
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(if (konumAcik) "Konumunuz paylaşılıyor" else "Konum paylaşımı kapalı", fontWeight = FontWeight.Bold)
                                    Text(
                                        if (konumAcik) "Telefon kilitliyken de gider. Mesai bitince kapatın." else "Açınca yöneticiniz sizi haritada görür, müşteri kuryesini takip eder.",
                                        color = Soluk, fontSize = 12.sp
                                    )
                                }
                                Switch(
                                    checked = konumAcik,
                                    onCheckedChange = { ac ->
                                        if (ac) konumAc() else { Konum.durdur(ctx); konumAcik = false; Oturum.konumKapatildi = System.currentTimeMillis() }
                                    },
                                    colors = SwitchDefaults.colors(checkedTrackColor = Yesil)
                                )
                            }
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OzetKutu("Üzerimde", bende.size.toString(), Modifier.weight(1f))
                            OzetKutu("Kasaya verilmemiş", ust.optInt("adet").toString(), Modifier.weight(1f))
                            OzetKutu("Üzerimdeki nakit", tl(ust.optDouble("nakit")), Modifier.weight(1.3f), Yesil)
                        }
                    }
                    if (mesaj != null) item {
                        Row(Modifier.fillMaxWidth().background(Koyu, RoundedCornerShape(12.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(mesaj!!, color = Color.White, modifier = Modifier.weight(1f))
                            TextButton(onClick = { mesaj = null }) { Text("Tamam", color = Color(0xFFFDBA74)) }
                        }
                    }
                    item { BolumBaslik("Üzerimdeki paketler (${bende.size})") }
                    if (bende.isEmpty()) item { BosKutu("Üzerinizde paket yok.") }
                    items(bende, key = { "b" + it.optInt("id") }) { p ->
                        PaketKart(p, benim = true, islemde = islemde == p.optInt("id"), teslim = { teslimPaket = p }, birak = { islem(p.optInt("id"), "birak") }, al = {})
                    }
                    item { BolumBaslik("Hazır / bekleyen paketler (${bekleyen.size})") }
                    if (bekleyen.isEmpty()) item { BosKutu("Bekleyen paket yok. Yeni paket gelince telefon titrer.") }
                    items(bekleyen, key = { "w" + it.optInt("id") }) { p ->
                        PaketKart(p, benim = false, islemde = islemde == p.optInt("id"), teslim = {}, birak = {}, al = { islem(p.optInt("id"), "al") })
                    }
                    if (bugun.isNotEmpty()) {
                        item { BolumBaslik("Bugün teslim ettiklerim (${bugun.size})") }
                        item {
                            Kart {
                                bugun.forEach { t ->
                                    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                                        Text(t.optString("saat"), color = Soluk, modifier = Modifier.width(48.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(t.optString("musteri").ifEmpty { t.optString("no") }, fontSize = 14.sp)
                                            if (t.optString("odeme").isNotEmpty()) Text(t.optString("odeme"), color = Soluk, fontSize = 12.sp)
                                        }
                                        Text(tl(t.optDouble("tutar")), fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val tp = teslimPaket
    if (tp != null) {
        val odendi = tp.optBoolean("odendi")
        AlertDialog(
            onDismissRequest = { teslimPaket = null },
            title = { Text(if (odendi) "Teslim edildi mi?" else "Ödeme nasıl alındı?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("${tp.optString("musteri").ifEmpty { "Müşteri" }} · ${tl(tp.optDouble("tutar"))}", color = Soluk)
                    if (odendi) {
                        Text("Bu sipariş önceden ödenmiş (online). Tahsilat yok.", fontSize = 14.sp)
                        BuyukDugme("✓ Teslim ettim", Yesil) { teslimPaket = null; islem(tp.optInt("id"), "teslim", "online") }
                    } else {
                        BuyukDugme("💵 Nakit aldım", Yesil) { teslimPaket = null; islem(tp.optInt("id"), "teslim", "nakit") }
                        BuyukDugme("💳 Kartla ödendi", Mavi) { teslimPaket = null; islem(tp.optInt("id"), "teslim", "kart") }
                        BuyukDugme("🌐 Online ödenmişti", Koyu2) { teslimPaket = null; islem(tp.optInt("id"), "teslim", "online") }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { teslimPaket = null }) { Text("Vazgeç", color = Soluk) } }
        )
    }
}

@Composable
private fun BuyukDugme(metin: String, renk: Color, tikla: () -> Unit) {
    Button(onClick = tikla, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = renk)) {
        Text(metin, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun OzetKutu(etiket: String, deger: String, modifier: Modifier, renk: Color = Murekkep) {
    Column(
        modifier.background(Color.White, RoundedCornerShape(14.dp)).padding(vertical = 12.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(deger, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = renk, maxLines = 1)
        Text(etiket, color = Soluk, fontSize = 11.sp, textAlign = TextAlign.Center, maxLines = 2)
    }
}

@Composable
private fun PaketKart(p: JSONObject, benim: Boolean, islemde: Boolean, teslim: () -> Unit, birak: () -> Unit, al: () -> Unit) {
    val ctx = LocalContext.current
    val tel = p.optString("telefon")
    val dk = p.optInt("dk")
    Kart {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(p.optString("musteri").ifEmpty { "Müşteri" }, Modifier.weight(1f), fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
            Text(tl(p.optDouble("tutar")), color = Turuncu, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            p.optString("adres").ifEmpty { if (!p.isNull("lat")) "📍 Konum linki ile gönderildi" else "Adres yok" } + (if (!p.isNull("lat")) "  📍" else ""),
            fontSize = 15.sp
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (p.optString("kanal").isNotEmpty()) Rozet(p.optString("kanal"), Color(0xFF9A3412), TuruncuAcik)
            Rozet(p.optString("no"), Soluk, Color(0xFFF5F5F4))
            Rozet("$dk dk", if (dk > 40) Kirmizi else Soluk, if (dk > 40) KirmiziAcik else Color(0xFFF5F5F4))
            if (p.optBoolean("odendi")) Rozet("Ödenmiş", Yesil, YesilAcik) else Rozet("Kapıda tahsil", Sari, SariAcik)
        }
        if (p.optString("not").isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text("📝 " + p.optString("not"), fontSize = 14.sp, modifier = Modifier.fillMaxWidth().background(Color(0xFFFFFBEB), RoundedCornerShape(10.dp)).padding(10.dp))
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (tel.isNotEmpty()) OutlinedButton(onClick = { runCatching { ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$tel"))) } }, modifier = Modifier.weight(1f)) {
                Text("📞 Ara", color = Murekkep)
            }
            val hedef = if (!p.isNull("lat")) "${p.optDouble("lat")},${p.optDouble("lng")}" else p.optString("adres")
            if (hedef.isNotBlank()) OutlinedButton(onClick = { haritaAc(ctx, hedef) }, modifier = Modifier.weight(1f)) { Text("🧭 Yol tarifi", color = Murekkep) }
            if (benim && tel.length >= 10) OutlinedButton(onClick = {
                val metin = if (p.isNull("lat")) "Merhaba, siparişiniz yola çıktı 🛵 Size daha hızlı ulaşabilmem için WhatsApp’tan konumunuzu paylaşır mısınız? 📍" else "Merhaba, siparişiniz kapınızda 🛵"
                runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/90" + tel.takeLast(10) + "?text=" + Uri.encode(metin)))) }
            }, modifier = Modifier.weight(1f)) { Text(if (p.isNull("lat")) "📍 Konum" else "💬 Kapıda", color = Murekkep) }
        }
        Spacer(Modifier.height(10.dp))
        if (islemde) {
            Box(Modifier.fillMaxWidth().height(54.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Turuncu) }
        } else if (benim) {
            BuyukDugme("✓ Teslim ettim", Yesil, teslim)
            TextButton(onClick = birak, modifier = Modifier.fillMaxWidth()) { Text("Bu paketi bırak", color = Soluk, fontSize = 13.sp) }
        } else {
            BuyukDugme("🛵 Aldım, yola çıkıyorum", Turuncu, al)
        }
    }
}

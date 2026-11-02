package com.teksosdoner.app.ui

import com.teksosdoner.app.*

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.teksosdoner.app.Oturum
import com.teksosdoner.app.TR
import com.teksosdoner.app.nesneler
import kotlinx.coroutines.launch

@Composable
fun ProfilEkrani() {
    val kapsam = rememberCoroutineScope()
    val benDurum by Oturum.ben.collectAsState()
    val ben = benDurum ?: return
    var yenile by remember { mutableIntStateOf(0) }
    var cikisSor by remember { mutableStateOf(false) }
    var mesaj by remember { mutableStateOf<String?>(null) }
    val bildirimAcik = remember(yenile) { os.bildirimAcik() }
    val pilSerbest = remember(yenile) { os.pilSerbest() }
    val subeler = ben.optJSONArray("subeler").nesneler()
    val admin = ben.optBoolean("admin")
    val kurye = ben.optBoolean("kurye") && !admin

    Ekran("Profil") {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Kart {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(56.dp).background(Turuncu, CircleShape), contentAlignment = Alignment.Center) {
                        Text(ben.optString("ad").take(1).uppercase(TR), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(ben.optString("ad"), fontWeight = FontWeight.ExtraBold, fontSize = 19.sp)
                        Text((if (admin) "Yönetici" else if (kurye) "Kurye" else "Personel") + " · " + ben.optString("sube_ad"), color = Soluk)
                        Text("@" + ben.optString("kullanici_adi"), color = Soluk, fontSize = 12.sp)
                    }
                }
            }
            if (subeler.size > 1) {
                BolumBaslik("Şube", Modifier.padding(start = 4.dp))
                Kart {
                    subeler.forEachIndexed { i, s ->
                        val kod = s.optString("kod")
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                kapsam.launch {
                                    Oturum.sube = kod
                                    runCatching { Api.get("ben") }.onSuccess { Oturum.benGuncelle(it.getJSONObject("ben")); mesaj = s.optString("ad") + " şubesine geçildi." }
                                        .onFailure { mesaj = it.message }
                                }
                            }.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = ben.optString("sube") == kod, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = Turuncu))
                            Spacer(Modifier.width(10.dp))
                            Text(s.optString("ad"), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                        if (i < subeler.size - 1) HorizontalDivider(color = Cizgi)
                    }
                    Text("Siparişler ve diğer sayfalar seçili şubeyi gösterir. Özet ekranında tüm şubeler birlikte görünür.", color = Soluk, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                }
            }
            BolumBaslik("Telefon ayarları", Modifier.padding(start = 4.dp))
            Kart {
                AyarSatir(
                    "Bildirimler", if (bildirimAcik) "Açık" else "Kapalı — yeni paket ve uyarılar gelmez", bildirimAcik,
                    if (bildirimAcik) null else "İzin ver"
                ) {
                    kapsam.launch {
                        runCatching { os.bildirimIzniIste(true) }
                        yenile++
                    }
                }
                if (os.pilAyariVar) HorizontalDivider(color = Cizgi)
                if (os.pilAyariVar) AyarSatir(
                    "Pil kısıtlaması", if (pilSerbest) "Kaldırıldı — arka planda çalışabilir" else "Açık — telefon uygulamayı uyutabilir", pilSerbest,
                    if (pilSerbest) null else "Kaldır"
                ) { os.pilIzniIste(); yenile++ }
                HorizontalDivider(color = Cizgi)
                AyarSatir("Uygulama izinleri", "Konum, kamera, bildirim", true, "Aç") {
                    runCatching { os.ayarlariAc() }
                }
            }
            if (mesaj != null) Rozet(mesaj!!, Yesil, YesilAcik)
            OutlinedButton(onClick = { yenile++ }, modifier = Modifier.fillMaxWidth()) { Text("Durumu yenile", color = Murekkep) }
            Button(
                onClick = { cikisSor = true }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = KirmiziAcik, contentColor = Kirmizi)
            ) { Text("Çıkış yap", fontWeight = FontWeight.Bold) }
            Text(
                "Teksos uygulaması ${os.surum} · ${os.ad} · sunucu ${ben.optString("sunucu_surum")}",
                color = Soluk, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 20.dp)
            )
        }
    }

    if (cikisSor) {
        AlertDialog(
            onDismissRequest = { cikisSor = false },
            title = { Text("Çıkış yapılsın mı?") },
            text = { Text(if (os.konumCalisiyor()) "Konum paylaşımı da kapanacak." else "Tekrar girmek için şifreniz gerekecek.") },
            confirmButton = {
                TextButton(onClick = {
                    cikisSor = false
                    kapsam.launch {
                        runCatching { Api.post("cikis") }
                        runCatching { os.bildirimCikis() }
                        runCatching { os.konumDurdur() }
                        Oturum.cikis()
                    }
                }) { Text("Çıkış yap", color = Kirmizi, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { cikisSor = false }) { Text("Vazgeç", color = Soluk) } }
        )
    }
}

@Composable
private fun AyarSatir(baslik: String, durum: String, iyi: Boolean, dugme: String?, tikla: () -> Unit) {
    val ikon = when {
        baslik.startsWith("Bildirim") -> Icons.Filled.Notifications
        baslik.startsWith("Pil") -> Icons.Filled.BatteryChargingFull
        else -> Icons.Filled.Security
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        IkonRozet(ikon, if (iyi) Yesil else Sari, 36)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(baslik, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(durum, color = if (iyi) Yesil else Sari, fontSize = 12.sp)
        }
        if (dugme != null) OutlinedButton(onClick = tikla) { Text(dugme, color = Turuncu) }
    }
}

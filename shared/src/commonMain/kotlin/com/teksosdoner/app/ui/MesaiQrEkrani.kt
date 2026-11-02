package com.teksosdoner.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import com.teksosdoner.app.json.JSONArray
import com.teksosdoner.app.json.JSONObject
import com.teksosdoner.app.nesneler
import com.teksosdoner.app.os
import kotlinx.coroutines.launch

/**
 * QR ile mesai girişi / çıkışı (yerli): konum → dükkandaki ekranın QR kodu → ön kamera fotoğrafı → gönder.
 * Çıkışta kapanış görevleri (gerekenlerde fotoğraf) işaretlenir.
 */
@Composable
fun MesaiQrEkrani(geri: () -> Unit) {
    val kapsam = rememberCoroutineScope()
    var veri by remember { mutableStateOf<JSONObject?>(null) }
    var hata by remember { mutableStateOf<String?>(null) }
    var adim by remember { mutableStateOf<String?>(null) }
    var sonuc by remember { mutableStateOf<String?>(null) }
    val onay = remember { mutableStateMapOf<Int, Boolean>() }
    val foto = remember { mutableStateMapOf<Int, String>() }

    suspend fun yukle() {
        try { veri = Api.get("mesai_qr"); hata = null } catch (e: ApiHata) { hata = e.message }
    }
    LaunchedEffect(Unit) { yukle() }

    val v = veri
    val aktif = v?.optJSONObject("aktif")
    val gorevler = v?.optJSONArray("gorevler").nesneler()
    val hazir = gorevler.all { g -> onay[g.optInt("id")] == true && (!g.optBoolean("foto") || foto.containsKey(g.optInt("id"))) }

    fun gonder(konum: String, qr: String, selfie: String?) {
        adim = "Gönderiliyor…"
        kapsam.launch {
            try {
                val (enlem, boylam) = konum.split(",").let { it[0].toDouble() to it[1].toDouble() }
                val g = JSONObject().put("qr", qr).put("enlem", enlem).put("boylam", boylam)
                if (selfie != null) g.put("selfie", selfie)
                g.put("gorevler", JSONArray(onay.filterValues { it }.keys.toList()))
                g.put("fotolar", JSONObject().apply { foto.forEach { (id, b) -> put(id.toString(), b) } })
                sonuc = Api.post("mesai_qr", g).optString("mesaj")
                hata = null
            } catch (e: ApiHata) {
                hata = e.message
            } catch (e: Exception) {
                hata = "Gönderilemedi: " + (e.message ?: "")
            } finally {
                adim = null
            }
        }
    }

    fun basla() {
        hata = null
        adim = "Konumunuz alınıyor…"
        os.konumAl { konum ->
            if (konum == null) {
                adim = null
                hata = "Konum alınamadı. Telefonun konum servisini ve uygulamanın konum iznini açıp tekrar deneyin."
                return@konumAl
            }
            adim = "Dükkandaki ekranın QR kodunu okutun…"
            os.qrOku { qr ->
                if (qr.isNullOrBlank()) {
                    adim = null
                    return@qrOku
                }
                adim = "Yüz fotoğrafınızı çekin…"
                os.fotoCek(true) { selfie -> gonder(konum, qr, selfie) }
            }
        }
    }

    Ekran(if (aktif != null) "Mesai çıkışı" else "Mesai girişi", geri = geri) {
        when {
            v == null && hata != null -> HataKutusu(hata!!) { kapsam.launch { yukle() } }
            v == null -> Yukleniyor()
            sonuc != null -> Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.CheckCircle, null, tint = Yesil, modifier = Modifier.size(84.dp))
                Spacer(Modifier.height(16.dp))
                Text(sonuc!!, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Murekkep)
                Spacer(Modifier.height(24.dp))
                Button(onClick = geri, colors = ButtonDefaults.buttonColors(containerColor = Turuncu), modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text("Tamam", fontWeight = FontWeight.Bold)
                }
            }
            else -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Kart(koyu = true) {
                    if (aktif != null) {
                        Text("ŞU AN MESAİDESİNİZ", color = Color(0xFF86EFAC), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("Giriş " + aktif.optString("giris"), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                        Text("Çıkmak için dükkandaki ekranın QR kodunu okutun.", color = Color(0xFFD6D3D1), fontSize = 13.sp)
                    } else {
                        Text("MESAİYE BAŞLA", color = Color(0xFFFDBA74), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("Dükkandaki ekranın QR kodunu okutun", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                        Text("Konumunuz (dükkana en fazla ${v.optInt("mesafe_siniri", 100)} m) ve yüz fotoğrafınız doğrulanır.", color = Color(0xFFD6D3D1), fontSize = 13.sp)
                    }
                }
                v.optString("unutulan").takeIf { it.isNotEmpty() && it != "null" }?.let {
                    Serit(Icons.Filled.Warning, "Önceki mesainin ($it) çıkışı yapılmamış; yöneticiniz düzeltecek.", Sari, SariAcik)
                }
                if (!v.optBoolean("dukkan_ayarli")) Serit(Icons.Filled.Warning, "Bu şubenin konumu henüz ayarlanmamış; yöneticiniz Ayarlar'dan kaydetmeli.", Kirmizi, KirmiziAcik)
                if (aktif != null && gorevler.isNotEmpty()) {
                    BolumBaslik(if (v.optBoolean("gece")) "Gece kapanış görevleri" else "Çıkış görevleri")
                    gorevler.forEach { g ->
                        val id = g.optInt("id")
                        Kart {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onay[id] = onay[id] != true }) {
                                Checkbox(checked = onay[id] == true, onCheckedChange = { onay[id] = it }, colors = CheckboxDefaults.colors(checkedColor = Yesil))
                                Text(g.optString("metin"), fontSize = 15.sp, modifier = Modifier.weight(1f))
                            }
                            if (g.optBoolean("foto")) {
                                OutlinedButton(onClick = { os.fotoCek(false) { b -> if (b != null) { foto[id] = b; onay[id] = true } } },
                                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                                    Icon(if (foto.containsKey(id)) Icons.Filled.CheckCircle else Icons.Filled.PhotoCamera, null,
                                        tint = if (foto.containsKey(id)) Yesil else Turuncu)
                                    Spacer(Modifier.width(8.dp))
                                    Text(if (foto.containsKey(id)) "Fotoğraf çekildi · yeniden çek" else "Fotoğraf çek (zorunlu)", color = Murekkep)
                                }
                            }
                        }
                    }
                }
                hata?.let { Text(it, color = Kirmizi, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth().background(KirmiziAcik, RoundedCornerShape(12.dp)).padding(12.dp)) }
                if (adim != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().background(MaviAcik, RoundedCornerShape(12.dp)).padding(12.dp)) {
                        CircularProgressIndicator(Modifier.size(20.dp), color = Mavi, strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(adim!!, color = Mavi, fontWeight = FontWeight.SemiBold)
                    }
                }
                Button(
                    onClick = { basla() }, enabled = adim == null && hazir,
                    colors = ButtonDefaults.buttonColors(containerColor = if (aktif != null) Kirmizi else Turuncu),
                    shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().height(58.dp)
                ) {
                    Icon(Icons.Filled.QrCodeScanner, null); Spacer(Modifier.width(10.dp))
                    Text(if (aktif != null) "QR okut · mesaiyi bitir" else "QR okut · mesaiye başla", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                if (!hazir) Text("Çıkış için önce bütün görevleri işaretleyin, fotoğraflı olanların fotoğrafını çekin.", color = Soluk, fontSize = 12.sp)
            }
        }
    }
}

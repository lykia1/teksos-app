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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teksosdoner.app.Api
import com.teksosdoner.app.ApiHata
import com.teksosdoner.app.TR
import com.teksosdoner.app.kisaTarih
import com.teksosdoner.app.nesneler
import com.teksosdoner.app.tl
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.teksosdoner.app.json.JSONObject

private fun ara(tel: String) = os.telefonAra(tel)

@Composable
fun MusterilerEkrani(geri: (() -> Unit)?) {
    val kapsam = rememberCoroutineScope()
    var aranan by rememberSaveable { mutableStateOf("") }
    var son by remember { mutableStateOf("") }
    var veri by remember { mutableStateOf<JSONObject?>(null) }
    var hata by remember { mutableStateOf<String?>(null) }
    var yukleniyor by remember { mutableStateOf(false) }
    suspend fun yukle() {
        yukleniyor = true
        try { veri = Api.get("musteriler", mapOf("ara" to son)); hata = null } catch (e: ApiHata) { hata = e.message } finally { yukleniyor = false }
    }
    LaunchedEffect(aranan) { delay(400); son = aranan.trim() }
    LaunchedEffect(son) { yukle() }
    Ekran("Paket Müşterileri", altBaslik = veri?.let { "${it.optInt("toplam")} kayıtlı müşteri · harcamaya göre" }, geri = geri) {
        Column(Modifier.fillMaxSize()) {
            OutlinedTextField(aranan, { aranan = it }, placeholder = { Text("Ad, telefon veya adres") }, singleLine = true, leadingIcon = { Icon(Icons.Filled.Search, null) },
                trailingIcon = { if (aranan.isNotEmpty()) IconButton(onClick = { aranan = "" }) { Icon(Icons.Filled.Clear, "Temizle") } },
                shape = RoundedCornerShape(14.dp), colors = alanRenkleri(), modifier = Modifier.fillMaxWidth().padding(14.dp))
            val v = veri
            when {
                v == null && hata != null -> HataKutusu(hata!!) { kapsam.launch { yukle() } }
                v == null -> Yukleniyor()
                else -> PullToRefreshBox(isRefreshing = yukleniyor, onRefresh = { kapsam.launch { yukle() } }, modifier = Modifier.fillMaxSize()) {
                    val liste = v.optJSONArray("liste").nesneler()
                    LazyColumn(contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                        if (liste.isEmpty()) item { BosKutu("Müşteri bulunamadı.") }
                        items(liste, key = { it.optInt("id") }) { c ->
                            Kart {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(42.dp).background(MaviAcik, CircleShape), contentAlignment = Alignment.Center) {
                                        Text(c.optString("ad").ifEmpty { "?" }.take(1).uppercase(TR), color = Mavi, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(c.optString("ad").ifEmpty { "İsimsiz" }, fontWeight = FontWeight.Bold)
                                        Text(c.optString("telefon"), color = Soluk, fontSize = 13.sp)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(tl(c.optDouble("harcama")), fontWeight = FontWeight.ExtraBold)
                                        Text("${c.optInt("siparis")} sipariş", color = Soluk, fontSize = 11.sp)
                                    }
                                }
                                if (c.optString("adres").isNotEmpty()) Text(c.optString("adres"), fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp), maxLines = 3)
                                if (c.optString("not").isNotEmpty()) Text("Not: " + c.optString("not"), color = Sari, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                                if (c.optString("son").length >= 10) Text("Son sipariş " + kisaTarih(c.optString("son")) + "." + c.optString("son").take(4), color = Soluk, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (c.optString("telefon").isNotEmpty()) OutlinedButton(onClick = { ara(c.optString("telefon")) }, modifier = Modifier.weight(1f)) {
                                        Icon(Icons.Filled.Call, null, tint = Turuncu, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Ara", color = Murekkep)
                                    }
                                    val hedef = if (!c.isNull("lat")) "${c.optDouble("lat")},${c.optDouble("lng")}" else c.optString("adres")
                                    if (hedef.isNotBlank()) OutlinedButton(onClick = { os.haritaAc(hedef) }, modifier = Modifier.weight(1f)) {
                                        Icon(Icons.Filled.Navigation, null, tint = Turuncu, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Yol tarifi", color = Murekkep)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PersonellerEkrani(geri: (() -> Unit)?) {
    val kapsam = rememberCoroutineScope()
    var veri by remember { mutableStateOf<JSONObject?>(null) }
    var hata by remember { mutableStateOf<String?>(null) }
    var yukleniyor by remember { mutableStateOf(false) }
    var pasif by rememberSaveable { mutableStateOf(false) }
    var mesaj by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    var kilit by remember { mutableStateOf<JSONObject?>(null) }
    suspend fun yukle() {
        yukleniyor = true
        try { veri = Api.get("personeller"); hata = null } catch (e: ApiHata) { hata = e.message } finally { yukleniyor = false }
    }
    LaunchedEffect(Unit) { yukle() }
    Ekran("Personeller", altBaslik = "Bilgiler ve cihaz kilidi", geri = geri) {
        val v = veri
        when {
            v == null && hata != null -> HataKutusu(hata!!) { kapsam.launch { yukle() } }
            v == null -> Yukleniyor()
            else -> PullToRefreshBox(isRefreshing = yukleniyor, onRefresh = { kapsam.launch { yukle() } }, modifier = Modifier.fillMaxSize()) {
                val tum = v.optJSONArray("liste").nesneler()
                val liste = tum.filter { it.optBoolean("aktif") != pasif }
                LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Cip2(!pasif, "Aktif (${tum.count { it.optBoolean("aktif") }})") { pasif = false }
                            Cip2(pasif, "Ayrılan / pasif (${tum.count { !it.optBoolean("aktif") }})") { pasif = true }
                        }
                    }
                    mesaj?.let { (m, h) -> item { MesajSeridi(m, h) { mesaj = null } } }
                    items(liste, key = { it.optInt("id") }) { p ->
                        Kart {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box {
                                    Box(Modifier.size(44.dp).background(TuruncuAcik, CircleShape), contentAlignment = Alignment.Center) {
                                        Text(p.optString("ad").take(1), color = Turuncu, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                                    }
                                    if (p.optBoolean("mesaide")) Box(Modifier.size(13.dp).align(Alignment.BottomEnd).background(Color.White, CircleShape).padding(2.dp).background(Yesil, CircleShape))
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(p.optString("ad"), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text(listOf(p.optString("pozisyon"), if (p.optBoolean("admin")) "Yönetici" else null, if (p.optBoolean("mesaide")) "şu an mesaide" else null)
                                        .filterNotNull().filter { it.isNotEmpty() }.joinToString(" · "), color = Soluk, fontSize = 12.sp)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(tl(p.optDouble("ucret")), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(p.optString("tip").lowercase(TR), color = Soluk, fontSize = 11.sp)
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            BilgiSatir("Bu ay onaylı mesai", String.format(TR, "%.1f saat", p.optDouble("ay_saat")))
                            if (p.optString("son_mesai").isNotEmpty()) BilgiSatir("Son mesai", kisaTarih(p.optString("son_mesai")) + "." + p.optString("son_mesai").take(4))
                            if (p.optString("kullanici").isNotEmpty()) BilgiSatir("Kullanıcı adı", p.optString("kullanici"))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                                if (p.optString("telefon").isNotEmpty()) OutlinedButton(onClick = { ara(p.optString("telefon")) }, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Filled.Call, null, tint = Turuncu, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Ara", color = Murekkep)
                                }
                                if (p.optBoolean("cihaz_kilitli") && !p.optBoolean("admin")) OutlinedButton(onClick = { kilit = p }, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Filled.LockReset, null, tint = Mavi, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Cihaz kilidi", color = Murekkep)
                                }
                            }
                        }
                    }
                    item { Text("Personel ekleme, ücret ve yetki düzenleme için Menü → Personeller (tam sayfa).", color = Soluk, fontSize = 12.sp, modifier = Modifier.padding(4.dp)) }
                }
            }
        }
    }
    kilit?.let { p ->
        OnayPenceresi("Cihaz kilidi sıfırlansın mı?", "${p.optString("ad")} bir sonraki girişini hangi telefondan yaparsa hesap o telefona kilitlenir. Telefon değiştiren personel için kullanın.", "Sıfırla", tehlikeli = false, kapat = { kilit = null }) {
            kapsam.launch {
                try { mesaj = Api.post("personel_islem", JSONObject().put("act", "kilit").put("id", p.optInt("id"))).optString("mesaj") to false } catch (e: ApiHata) { mesaj = (e.message ?: "Hata") to true }
                yukle()
            }
        }
    }
}

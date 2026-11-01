package com.teksosdoner.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teksosdoner.app.Api
import com.teksosdoner.app.ApiHata
import com.teksosdoner.app.Oturum
import com.teksosdoner.app.R
import com.teksosdoner.app.nesneler
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun GirisEkrani() {
    val kapsam = rememberCoroutineScope()
    var subeler by remember { mutableStateOf(listOf<Pair<String, String>>()) }
    var marka by remember { mutableStateOf("Teksos Döner") }
    var sube by remember { mutableStateOf("ana") }
    var kadi by remember { mutableStateOf("") }
    var sifre by remember { mutableStateOf("") }
    var kod by remember { mutableStateOf("") }
    var bekleme by remember { mutableStateOf<String?>(null) }
    var bilgi by remember { mutableStateOf<String?>(null) }
    var hata by remember { mutableStateOf<String?>(null) }
    var bekliyor by remember { mutableStateOf(false) }
    var subeAcik by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        runCatching { Api.get("subeler") }.onSuccess { j ->
            subeler = j.optJSONArray("subeler").nesneler().map { it.getString("kod") to it.getString("ad") }
            marka = j.optString("marka", marka)
            sube = subeler.firstOrNull()?.first ?: "ana"
        }
    }

    fun gonder() {
        if (bekliyor) return
        kapsam.launch {
            bekliyor = true
            hata = null
            try {
                val j = if (bekleme == null) {
                    Api.post("giris", JSONObject().put("kullanici_adi", kadi.trim()).put("sifre", sifre).put("sube", sube).put("cihaz", Oturum.cihaz))
                } else {
                    Api.post("giris_kod", JSONObject().put("bekleme", bekleme).put("kod", kod).put("sube", sube))
                }
                if (j.optBoolean("iki_adim")) {
                    bekleme = j.getString("bekleme")
                    bilgi = j.optString("mesaj")
                } else {
                    Oturum.sube = null
                    Oturum.girisYap(j.getString("anahtar"), j.getJSONObject("ben"))
                }
            } catch (e: ApiHata) {
                hata = e.message
                if (e.kod == 401 && bekleme != null && e.message?.contains("süresi") == true) bekleme = null
            } finally {
                bekliyor = false
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Koyu)) {
        Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState())) {
            Column(
                Modifier.fillMaxWidth().statusBarsPadding().padding(top = 56.dp, bottom = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier.size(width = 230.dp, height = 170.dp).clip(RoundedCornerShape(28.dp)).background(Color.White).padding(18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(painterResource(R.drawable.logo), contentDescription = marka, modifier = Modifier.fillMaxSize())
                }
                Spacer(Modifier.height(16.dp))
                Text("Yönetim · Kasa · Kurye", color = Color(0xFFD6D3D1), fontSize = 15.sp)
            }
            Spacer(Modifier.weight(1f))
            Surface(color = Color.White, shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(24.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(if (bekleme == null) "Giriş yapın" else "Doğrulama kodu", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                    val alanRenk = OutlinedTextFieldDefaults.colors(focusedBorderColor = Turuncu, focusedLabelColor = Turuncu, cursorColor = Turuncu)
                    if (bekleme == null) {
                        if (subeler.size > 1) {
                            Box {
                                OutlinedButton(onClick = { subeAcik = true }, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(12.dp)) {
                                    Text("🏪  " + (subeler.firstOrNull { it.first == sube }?.second ?: sube), Modifier.weight(1f), color = Murekkep, fontSize = 16.sp)
                                    Icon(Icons.Filled.ArrowDropDown, null, tint = Soluk)
                                }
                                DropdownMenu(expanded = subeAcik, onDismissRequest = { subeAcik = false }) {
                                    subeler.forEach { (k, a) -> DropdownMenuItem(text = { Text(a) }, onClick = { sube = k; subeAcik = false }) }
                                }
                            }
                        }
                        OutlinedTextField(
                            kadi, { kadi = it }, label = { Text("Kullanıcı adı") }, singleLine = true, colors = alanRenk,
                            shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Next, autoCorrectEnabled = false)
                        )
                        OutlinedTextField(
                            sifre, { sifre = it }, label = { Text("Şifre") }, singleLine = true, colors = alanRenk,
                            shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(),
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { gonder() })
                        )
                    } else {
                        if (bilgi != null) Text("📲 $bilgi", color = Mavi, fontSize = 14.sp)
                        OutlinedTextField(
                            kod, { kod = it.filter { c -> c.isDigit() }.take(6) }, label = { Text("6 haneli kod") }, singleLine = true, colors = alanRenk,
                            shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 24.sp, letterSpacing = 6.sp, textAlign = TextAlign.Center),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { gonder() })
                        )
                    }
                    if (hata != null) Text(hata!!, color = Kirmizi, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Button(
                        onClick = { gonder() }, enabled = !bekliyor && (if (bekleme == null) kadi.isNotBlank() && sifre.isNotEmpty() else kod.length == 6),
                        modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Turuncu)
                    ) {
                        if (bekliyor) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                        else Text(if (bekleme == null) "Giriş Yap" else "Doğrula ve gir", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    }
                    if (bekleme != null) {
                        TextButton(onClick = { bekleme = null; kod = ""; hata = null }, modifier = Modifier.fillMaxWidth()) { Text("Vazgeç, baştan giriş yap", color = Soluk) }
                    }
                }
            }
        }
    }
}

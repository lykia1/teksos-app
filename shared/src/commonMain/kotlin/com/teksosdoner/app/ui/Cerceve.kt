@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.teksosdoner.app.ui

import com.teksosdoner.app.*

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teksosdoner.app.Api
import com.teksosdoner.app.ApiHata
import com.teksosdoner.app.gunAdi
import com.teksosdoner.app.kisaTarih
import kotlinx.coroutines.launch
import com.teksosdoner.app.json.JSONObject

/** Ekranın işlemleri: sunucuya gönder + yenile, pencere aç/kapat. */
class Kontrol(
    val islem: (rota: String, govde: JSONObject) -> Unit,
    val ac: (@Composable () -> Unit) -> Unit,
    val kapat: () -> Unit,
    val yenile: () -> Unit,
)

/**
 * Yerli ekran çatısı: veriyi yükler, aşağı çekince yeniler, işlem sonucunu şeritte gösterir,
 * pencereleri (form / onay) yönetir. icerik: listenin kendisi.
 */
@Composable
fun VeriEkrani(
    baslik: String,
    rota: String,
    geri: (() -> Unit)?,
    altBaslik: ((JSONObject) -> String?)? = null,
    parametre: Map<String, String> = emptyMap(),
    ust: (@Composable (JSONObject, Kontrol) -> Unit)? = null,
    fab: Pair<String, (JSONObject, Kontrol) -> Unit>? = null,
    icerik: LazyListScope.(JSONObject, Kontrol) -> Unit,
) {
    val kapsam = rememberCoroutineScope()
    var veri by remember { mutableStateOf<JSONObject?>(null) }
    var hata by remember { mutableStateOf<String?>(null) }
    var yukleniyor by remember { mutableStateOf(false) }
    var mesaj by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    var pencere by remember { mutableStateOf<(@Composable () -> Unit)?>(null) }
    suspend fun yukle() {
        yukleniyor = true
        try { veri = Api.get(rota, parametre); hata = null } catch (e: ApiHata) { hata = e.message } finally { yukleniyor = false }
    }
    val kontrol = remember(rota, parametre) {
        Kontrol(
            islem = { r, g ->
                pencere = null
                kapsam.launch {
                    try { mesaj = Api.post(r, g).optString("mesaj", "Kaydedildi.") to false } catch (e: ApiHata) { mesaj = (e.message ?: "Hata") to true }
                    yukle()
                }
            },
            ac = { pencere = it }, kapat = { pencere = null }, yenile = { kapsam.launch { yukle() } }
        )
    }
    LaunchedEffect(rota, parametre) { yukle() }
    Ekran(baslik, altBaslik = veri?.let { altBaslik?.invoke(it) }, geri = geri, eylemler = {
        IconButton(onClick = { kapsam.launch { yukle() } }) { Icon(Icons.Filled.Refresh, "Yenile") }
    }) {
        Box(Modifier.fillMaxSize()) {
            val v = veri
            when {
                v == null && hata != null -> HataKutusu(hata!!) { kapsam.launch { yukle() } }
                v == null -> Yukleniyor()
                else -> Column(Modifier.fillMaxSize()) {
                    ust?.invoke(v, kontrol)
                    PullToRefreshBox(isRefreshing = yukleniyor, onRefresh = { kapsam.launch { yukle() } }, modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = if (fab != null) 96.dp else 24.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()
                        ) {
                            mesaj?.let { (m, h) -> item { MesajSeridi(m, h) { mesaj = null } } }
                            if (hata != null) item { MesajSeridi(hata, true) { hata = null } }
                            icerik(v, kontrol)
                        }
                    }
                }
            }
            if (fab != null && v != null) ExtendedFloatingActionButton(
                onClick = { fab.second(v, kontrol) }, containerColor = Turuncu, contentColor = Color.White,
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                icon = { Icon(Icons.Filled.Add, null) }, text = { Text(fab.first, fontWeight = FontWeight.Bold) }
            )
        }
    }
    pencere?.invoke()
}

enum class AlanTip { METIN, SAYI, SECIM, ANAHTAR, UZUN, SAAT, TARIH, COKLU }

/** Form alanı. COKLU: secenekler içinden birden fazla seçim (değer virgülle ayrılır). */
data class Alan(
    val ad: String, val etiket: String, val tip: AlanTip = AlanTip.METIN, val deger: String = "",
    val secenekler: List<Pair<String, String>> = emptyList(), val ipucu: String? = null,
)

/** Genel form penceresi; kaydet'e alan adı → değer haritası gelir. */
@Composable
fun FormPenceresi(
    baslik: String, alanlar: List<Alan>, dugme: String = "Kaydet", renk: Color = Turuncu,
    kapat: () -> Unit, kaydet: (Map<String, String>) -> Unit,
) {
    val d = remember { mutableStateMapOf<String, String>().apply { alanlar.forEach { put(it.ad, it.deger) } } }
    AlertDialog(
        onDismissRequest = kapat,
        title = { Text(baslik, fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                alanlar.forEach { a ->
                    val v = d[a.ad].orEmpty()
                    when (a.tip) {
                        AlanTip.SECIM -> SecimKutusu(a.etiket, a.secenekler, v) { d[a.ad] = it }
                        AlanTip.ANAHTAR -> Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(a.etiket, fontWeight = FontWeight.SemiBold)
                                if (a.ipucu != null) Text(a.ipucu, color = Soluk, fontSize = 12.sp)
                            }
                            Switch(checked = v == "1", onCheckedChange = { d[a.ad] = if (it) "1" else "" }, colors = SwitchDefaults.colors(checkedTrackColor = Turuncu))
                        }
                        AlanTip.SAAT -> OutlinedButton(onClick = {
                            saatSec(v.ifEmpty { "11:00" }) { d[a.ad] = it }
                        }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                            Text(a.etiket + ": " + v.ifEmpty { "seçin" }, color = Murekkep)
                        }
                        AlanTip.TARIH -> OutlinedButton(onClick = {
                            tarihSec(a.etiket, if (v.length >= 10) v else bugunTarih()) { d[a.ad] = it }
                        }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                            Text(a.etiket + ": " + (if (v.length >= 10) kisaTarih(v) + "." + v.take(4) + " " + gunAdi(v) else "seçin"), color = Murekkep)
                        }
                        AlanTip.COKLU -> Column {
                            Text(a.etiket, color = Soluk, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 4.dp))
                            val secili = v.split(",").filter { it.isNotEmpty() }.toSet()
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                a.secenekler.forEach { (k, ad) ->
                                    Cip2(k in secili, ad) { d[a.ad] = (if (k in secili) secili - k else secili + k).joinToString(",") }
                                }
                            }
                        }
                        else -> OutlinedTextField(
                            v, { d[a.ad] = if (a.tip == AlanTip.SAYI) it.filter { c -> c.isDigit() || c == ',' || c == '.' || c == '-' } else it },
                            label = { Text(a.etiket) }, singleLine = a.tip != AlanTip.UZUN, minLines = if (a.tip == AlanTip.UZUN) 3 else 1,
                            supportingText = if (a.ipucu != null) ({ Text(a.ipucu) }) else null,
                            keyboardOptions = KeyboardOptions(keyboardType = if (a.tip == AlanTip.SAYI) KeyboardType.Decimal else KeyboardType.Text),
                            colors = alanRenkleri(), modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { kaydet(d.toMap()) }, colors = ButtonDefaults.buttonColors(containerColor = renk)) { Text(dugme, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = kapat) { Text("Vazgeç", color = Soluk) } }
    )
}

/** Haritayı JSON gövdesine çevirir (act ve id ile). */
fun govde(act: String? = null, id: Int? = null, alanlar: Map<String, String> = emptyMap()): JSONObject =
    JSONObject().apply {
        act?.let { put("act", it) }
        id?.let { put("id", it) }
        alanlar.forEach { (k, v) -> put(k, v) }
    }

/** Satır sonu küçük eylem düğmeleri. */
@Composable
fun EylemSatiri(vararg eylemler: Triple<String, Color, () -> Unit>) {
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.End) {
        eylemler.forEach { (ad, renk, tikla) ->
            TextButton(onClick = tikla) { Text(ad, color = renk, fontWeight = FontWeight.SemiBold) }
            Spacer(Modifier.width(2.dp))
        }
    }
}

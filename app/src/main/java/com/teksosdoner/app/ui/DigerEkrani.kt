package com.teksosdoner.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teksosdoner.app.Api
import com.teksosdoner.app.ApiHata
import com.teksosdoner.app.nesneler
import kotlinx.coroutines.launch
import org.json.JSONObject

private val ikonlar = mapOf(
    "pano" to "📊", "patron" to "📈", "panelim" to "📱", "kasa" to "🧾", "siparisler" to "🧾", "musteriler" to "📇",
    "kampanyalar" to "🎁", "kuryeler" to "🛵", "kurye" to "🛵", "urunler" to "🍔", "kategoriler" to "🗂", "secenekler" to "➕",
    "fiyatlar" to "🏷", "tv" to "📺", "satis_rapor" to "📊", "gunsonu" to "🧮", "rapor" to "⚖️", "muhasebe" to "📑",
    "gider" to "💸", "stok" to "📦", "doner" to "🔥", "personel" to "🪪", "mesai" to "🕘", "vardiya" to "🗓", "avans" to "👛",
    "hijyen" to "🧼", "bildirim" to "📣", "ayarlar" to "⚙️", "subeler" to "🏪", "entegrasyon" to "🔌", "yemekpos" to "🔁",
    "islemler" to "📜", "odeme" to "💳", "masalar" to "🪑"
)

/** Tüm sayfalar: yerli olanlar uygulamanın kendi ekranında, diğerleri uygulama içinde açılır. */
@Composable
fun DigerEkrani(ac: (yerli: String?, url: String, ad: String) -> Unit) {
    val kapsam = rememberCoroutineScope()
    var gruplar by remember { mutableStateOf<List<JSONObject>?>(null) }
    var hata by remember { mutableStateOf<String?>(null) }
    suspend fun yukle() {
        try { gruplar = Api.get("menu").optJSONArray("gruplar").nesneler(); hata = null } catch (e: ApiHata) { hata = e.message }
    }
    LaunchedEffect(Unit) { yukle() }
    Ekran("Menü", altBaslik = "Tüm bölümler") {
        val g = gruplar
        when {
            g == null && hata != null -> HataKutusu(hata!!) { kapsam.launch { yukle() } }
            g == null -> Yukleniyor()
            else -> LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
                items(g) { grup ->
                    BolumBaslik(grup.optString("ad"), Modifier.padding(start = 4.dp))
                    Kart {
                        val ogeler = grup.optJSONArray("ogeler").nesneler()
                        ogeler.forEachIndexed { i, o ->
                            Row(
                                Modifier.fillMaxWidth().clickable {
                                    ac(o.optString("yerli").takeIf { o.has("yerli") && !o.isNull("yerli") && it.isNotEmpty() }, o.optString("url"), o.optString("ad"))
                                }.padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(ikonlar[o.optString("anahtar")] ?: "•", fontSize = 20.sp, modifier = Modifier.padding(end = 14.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(o.optString("ad"), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Soluk)
                            }
                            if (i < ogeler.size - 1) HorizontalDivider(color = Cizgi)
                        }
                    }
                }
            }
        }
    }
}

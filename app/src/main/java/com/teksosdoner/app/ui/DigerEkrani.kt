package com.teksosdoner.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Chair
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Widgets
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teksosdoner.app.Api
import com.teksosdoner.app.ApiHata
import com.teksosdoner.app.nesneler
import kotlinx.coroutines.launch
import org.json.JSONObject

private val Ikonlar: Map<String, ImageVector> = mapOf(
    "pano" to Icons.Filled.Dashboard, "patron" to Icons.Filled.Insights, "panelim" to Icons.Filled.PhoneAndroid,
    "kasa" to Icons.Filled.PointOfSale, "siparisler" to Icons.Filled.ReceiptLong, "musteriler" to Icons.Filled.ContactPhone,
    "kampanyalar" to Icons.Filled.CardGiftcard, "kuryeler" to Icons.Filled.DeliveryDining, "kurye" to Icons.Filled.DeliveryDining,
    "urunler" to Icons.Filled.Restaurant, "kategoriler" to Icons.Filled.Category, "secenekler" to Icons.Filled.PlaylistAdd,
    "fiyatlar" to Icons.Filled.Sell, "tv" to Icons.Filled.Tv, "satis_rapor" to Icons.Filled.Insights, "gunsonu" to Icons.Filled.Calculate,
    "rapor" to Icons.Filled.Scale, "muhasebe" to Icons.Filled.Description, "gider" to Icons.Filled.AccountBalanceWallet,
    "stok" to Icons.Filled.Inventory2, "doner" to Icons.Filled.LocalFireDepartment, "personel" to Icons.Filled.Badge,
    "mesai" to Icons.Filled.Schedule, "vardiya" to Icons.Filled.CalendarMonth, "avans" to Icons.Filled.AccountBalance,
    "hijyen" to Icons.Filled.CleaningServices, "bildirim" to Icons.Filled.Campaign, "ayarlar" to Icons.Filled.Settings,
    "subeler" to Icons.Filled.Storefront, "entegrasyon" to Icons.Filled.Hub, "yemekpos" to Icons.Filled.SyncAlt,
    "islemler" to Icons.Filled.History, "odeme" to Icons.Filled.CreditCard, "masalar" to Icons.Filled.Chair,
)

private val GrupRenk = mapOf(
    "Genel" to Turuncu, "Satış" to Color(0xFF1D4ED8), "Menü" to Color(0xFFDB2777), "Raporlar" to Color(0xFF7C3AED),
    "Finans & Depo" to Color(0xFF15803D), "Personel" to Color(0xFF0E7490), "Ayarlar" to Color(0xFF57534E)
)

/** Tüm bölümler: kutucuklar; yerli olanlar uygulamanın kendi ekranında, diğerleri uygulama içinde açılır. */
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
            else -> LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxSize()) {
                items(g) { grup ->
                    val renk = GrupRenk[grup.optString("ad")] ?: Turuncu
                    BolumBaslik(grup.optString("ad"), Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp))
                    val ogeler = grup.optJSONArray("ogeler").nesneler()
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ogeler.chunked(3).forEach { satir ->
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                satir.forEach { o ->
                                    Column(
                                        Modifier.weight(1f).height(104.dp)
                                            .background(Color.White, RoundedCornerShape(18.dp))
                                            .clickable {
                                                val yerli = if (o.isNull("yerli")) null else o.optString("yerli").ifEmpty { null }
                                                ac(yerli, o.optString("url"), o.optString("ad"))
                                            }
                                            .padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        IkonRozet(Ikonlar[o.optString("anahtar")] ?: Icons.Filled.Widgets, renk, 42)
                                        Spacer(Modifier.height(8.dp))
                                        Text(o.optString("ad"), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, maxLines = 2, lineHeight = 14.sp, color = Murekkep)
                                    }
                                }
                                repeat(3 - satir.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

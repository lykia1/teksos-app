package com.teksosdoner.app.ui

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.onesignal.OneSignal
import com.teksosdoner.app.Api
import com.teksosdoner.app.Bildirim
import com.teksosdoner.app.Oturum

private data class Sekme(val rota: String, val ad: String, val ikon: ImageVector)

/** Web sayfasını uygulama içinde açan rota. */
fun NavHostController.webAc(url: String, baslik: String) {
    navigate("web?url=" + Uri.encode(url) + "&baslik=" + Uri.encode(baslik))
}

@Composable
fun AnaEkran() {
    val benDurum by Oturum.ben.collectAsState()
    val ben = benDurum ?: return
    val admin = ben.optBoolean("admin")
    val kurye = ben.optBoolean("kurye") && !admin
    val kasa = ben.optJSONObject("yetki")?.optBoolean("kasa") == true
    val sekmeler = buildList {
        if (admin) add(Sekme("patron", "Özet", Icons.Filled.Dashboard))
        if (admin || kasa) add(Sekme("siparisler", "Siparişler", Icons.Filled.ReceiptLong))
        if (admin) add(Sekme("raporlar", "Raporlar", Icons.Filled.Insights))
        if (kurye) add(Sekme("kurye", "Paketler", Icons.Filled.DeliveryDining))
        if (!admin) add(Sekme("panel", "Panelim", Icons.Filled.Badge))
        add(Sekme("diger", "Menü", Icons.Filled.Apps))
        add(Sekme("profil", "Profil", Icons.Filled.AccountCircle))
    }
    val nav = rememberNavController()
    val girdi by nav.currentBackStackEntryAsState()
    val rota = girdi?.destination?.route

    // Kullanıcı bilgisini tazele, bildirim kimliğini bağla
    LaunchedEffect(Unit) {
        runCatching { Api.get("ben") }.onSuccess { Oturum.benGuncelle(it.getJSONObject("ben")) }
    }
    val pushId = ben.optString("push_id")
    LaunchedEffect(pushId) {
        if (pushId.isNotEmpty()) {
            runCatching { OneSignal.login(pushId) }
            runCatching { OneSignal.Notifications.requestPermission(false) }
        }
    }
    // Bildirime dokunulduysa ilgili ekran
    val bekleyen by Bildirim.bekleyen.collectAsState()
    LaunchedEffect(bekleyen) {
        val url = bekleyen ?: return@LaunchedEffect
        Bildirim.bekleyen.value = null
        when {
            url.contains("kurye.php") && kurye -> nav.navigate("kurye") { launchSingleTop = true }
            url.contains("patron.php") && admin -> nav.navigate("patron") { launchSingleTop = true }
            else -> nav.webAc(if (url.startsWith("http")) url else Api.sunucu + url.trimStart('/'), "Teksos")
        }
    }

    Scaffold(
        bottomBar = {
            if (sekmeler.any { it.rota == rota } || rota == null) {
                NavigationBar(containerColor = Color.White, tonalElevation = 0.dp) {
                    sekmeler.forEach { s ->
                        NavigationBarItem(
                            selected = rota == s.rota,
                            onClick = {
                                nav.navigate(s.rota) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(s.ikon, contentDescription = s.ad) },
                            label = { Text(s.ad, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Turuncu, selectedTextColor = Turuncu, indicatorColor = TuruncuAcik,
                                unselectedIconColor = Soluk, unselectedTextColor = Soluk
                            )
                        )
                    }
                }
            }
        },
        containerColor = Zemin
    ) { ic ->
        NavHost(nav, startDestination = sekmeler.first().rota, modifier = Modifier.padding(bottom = ic.calculateBottomPadding())) {
            composable("patron") { PatronEkrani() }
            composable("siparisler") { SiparislerEkrani(ac = { nav.navigate("siparis/$it") }) }
            composable("siparis/{id}", arguments = listOf(navArgument("id") { type = NavType.IntType })) {
                SiparisDetay(it.arguments?.getInt("id") ?: 0, geri = { nav.popBackStack() })
            }
            composable("raporlar") { RaporlarEkrani() }
            composable("kurye") { KuryeEkrani() }
            composable("panel") { WebEkrani(Api.sunucu + "personel_panel.php", "Panelim", geri = null) }
            composable("diger") {
                DigerEkrani(ac = { yerli, url, ad ->
                    if (yerli != null && sekmeler.any { s -> s.rota == yerli }) {
                        nav.navigate(yerli) {
                            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    } else nav.webAc(url, ad)
                })
            }
            composable(
                "web?url={url}&baslik={baslik}",
                arguments = listOf(
                    navArgument("url") { type = NavType.StringType; defaultValue = "" },
                    navArgument("baslik") { type = NavType.StringType; defaultValue = "" }
                )
            ) {
                WebEkrani(it.arguments?.getString("url").orEmpty(), it.arguments?.getString("baslik").orEmpty(), geri = { nav.popBackStack() })
            }
            composable("profil") { ProfilEkrani() }
        }
    }
}


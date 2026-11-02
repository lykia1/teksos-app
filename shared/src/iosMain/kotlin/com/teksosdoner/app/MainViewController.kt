package com.teksosdoner.app

import androidx.compose.ui.window.ComposeUIViewController
import com.teksosdoner.app.ui.Uygulama
import platform.UIKit.UIViewController

/** Swift'ten çağrılır: uygulamanın tek ekranı (tüm ekranlar Compose ile çizilir). */
fun MainViewController(kopru: IosKopru): UIViewController {
    os = IosPlatform(kopru)
    Oturum.baslat()
    return ComposeUIViewController { Uygulama() }
}

/** Bildirime dokunulunca Swift buraya bağlantıyı verir (ör. kurye.php → Paketler). */
fun bildirimeDokunuldu(url: String) {
    if (url.isNotEmpty()) Bildirim.bekleyen.value = url
}

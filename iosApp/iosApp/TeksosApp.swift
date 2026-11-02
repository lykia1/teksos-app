import SwiftUI
import UIKit
import UserNotifications
import Shared

/// iPhone uygulamasının girişi. Bütün ekranlar ortak (Kotlin/Compose) koddan gelir;
/// burada yalnızca bildirimler, konum ve yazdırma gibi Apple'a özgü işler var (Kopru.swift, Bildirim.swift).
@main
struct TeksosApp: App {
    @UIApplicationDelegateAdaptor(UygulamaTemsilcisi.self) var temsilci

    var body: some Scene {
        WindowGroup {
            KokGorunum().ignoresSafeArea()
        }
    }
}

struct KokGorunum: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController(kopru: Kopru.ortak)
    }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

final class UygulamaTemsilcisi: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil) -> Bool {
        UNUserNotificationCenter.current().delegate = self
        Bildirimler.ortak.baslat()
        Kopru.ortak.kaldigiYerdenSurdur()
        return true
    }

    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        Bildirimler.ortak.cihazAnahtari(deviceToken)
    }

    func application(_ application: UIApplication, didFailToRegisterForRemoteNotificationsWithError error: Error) {
        NSLog("Teksos bildirim kaydı başarısız: \(error.localizedDescription)")
    }

    func applicationDidBecomeActive(_ application: UIApplication) {
        Bildirimler.ortak.durumuYenile()
    }

    // Uygulama açıkken de bildirim görünsün
    func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification,
                                withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        completionHandler([.banner, .list, .sound])
    }

    // Bildirime dokunulunca ilgili ekran (ör. kurye.php → Paketler)
    func userNotificationCenter(_ center: UNUserNotificationCenter, didReceive response: UNNotificationResponse,
                                withCompletionHandler completionHandler: @escaping () -> Void) {
        if let url = Bildirimler.baglanti(response.notification.request.content.userInfo) {
            MainViewControllerKt.bildirimeDokunuldu(url: url)
        }
        completionHandler()
    }
}

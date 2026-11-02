import SwiftUI
import UIKit
import Shared
import OneSignalFramework

/// iPhone uygulamasının girişi. Bütün ekranlar ortak (Kotlin/Compose) koddan gelir;
/// burada yalnızca bildirimler, konum ve yazdırma gibi Apple'a özgü işler var (Kopru.swift).
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

final class UygulamaTemsilcisi: NSObject, UIApplicationDelegate, OSNotificationClickListener {
    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil) -> Bool {
        OneSignal.initialize("7fb5f933-2168-419a-8fe8-c4b9cfad13b4", withLaunchOptions: launchOptions)
        OneSignal.Notifications.addClickListener(self)
        Kopru.ortak.kaldigiYerdenSurdur()
        return true
    }

    func onClick(event: OSNotificationClickEvent) {
        if let url = event.notification.additionalData?["url"] as? String {
            MainViewControllerKt.bildirimeDokunuldu(url: url)
        }
    }
}

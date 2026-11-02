import Foundation
import UIKit
import UserNotifications

/// Bildirimler: Apple'ın kendi bildirim sistemi (APNs) + OneSignal'in web arayüzü.
/// OneSignal'in iPhone kütüphanesi kullanılmaz; cihaz anahtarı OneSignal'e doğrudan kaydedilir.
/// Sunucu (push_gonder) bildirimleri yine OneSignal üzerinden "external_id" ile gönderir.
final class Bildirimler {
    static let ortak = Bildirimler()
    private let uygulamaKimligi = "7fb5f933-2168-419a-8fe8-c4b9cfad13b4"
    private let ayar = UserDefaults.standard
    private(set) var izinVar = false

    func baslat() {
        durumuYenile()
        // İzin daha önce verildiyse cihaz anahtarını her açılışta tazele
        UNUserNotificationCenter.current().getNotificationSettings { s in
            if s.authorizationStatus == .authorized || s.authorizationStatus == .provisional {
                DispatchQueue.main.async { UIApplication.shared.registerForRemoteNotifications() }
            }
        }
    }

    func durumuYenile() {
        UNUserNotificationCenter.current().getNotificationSettings { s in
            self.izinVar = s.authorizationStatus == .authorized || s.authorizationStatus == .provisional
        }
    }

    func izinIste(ayaraGotur: Bool) {
        UNUserNotificationCenter.current().getNotificationSettings { s in
            if s.authorizationStatus == .denied {
                if ayaraGotur, let u = URL(string: UIApplication.openSettingsURLString) {
                    DispatchQueue.main.async { UIApplication.shared.open(u) }
                }
                return
            }
            UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge]) { verildi, _ in
                self.izinVar = verildi
                if verildi { DispatchQueue.main.async { UIApplication.shared.registerForRemoteNotifications() } }
            }
        }
    }

    /// Giriş yapan kişinin bildirim kimliği (ör. "personel_ana_12", "yonetici_admin")
    func giris(_ kimlik: String) {
        ayar.set(kimlik, forKey: "bildirim_kimlik")
        kaydet()
    }

    func cikis() {
        ayar.removeObject(forKey: "bildirim_kimlik")
    }

    func cihazAnahtari(_ veri: Data) {
        ayar.set(veri.map { String(format: "%02x", $0) }.joined(), forKey: "apns_anahtar")
        kaydet()
    }

    /// Cihazı OneSignal'e, kişinin kimliğiyle kaydeder
    private func kaydet() {
        guard let kimlik = ayar.string(forKey: "bildirim_kimlik"), !kimlik.isEmpty,
              let anahtar = ayar.string(forKey: "apns_anahtar"), !anahtar.isEmpty,
              let url = URL(string: "https://api.onesignal.com/apps/\(uygulamaKimligi)/users") else { return }
        var istek = URLRequest(url: url)
        istek.httpMethod = "POST"
        istek.setValue("application/json", forHTTPHeaderField: "Content-Type")
        let govde: [String: Any] = [
            "identity": ["external_id": kimlik],
            "subscriptions": [[
                "type": "iOSPush", "token": anahtar, "enabled": true, "notification_types": 1,
                "app_version": Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "",
                "device_model": "iPhone", "device_os": UIDevice.current.systemVersion,
            ]],
        ]
        istek.httpBody = try? JSONSerialization.data(withJSONObject: govde)
        URLSession.shared.dataTask(with: istek) { _, yanit, _ in
            NSLog("Teksos bildirim kaydı: \((yanit as? HTTPURLResponse)?.statusCode ?? 0)")
        }.resume()
    }

    /// Bildirimdeki bağlantı (OneSignal ek verisi "custom.a.url" içinde gelir)
    static func baglanti(_ veri: [AnyHashable: Any]) -> String? {
        if let c = veri["custom"] as? [String: Any], let a = c["a"] as? [String: Any], let u = a["url"] as? String { return u }
        if let u = veri["url"] as? String { return u }
        return nil
    }
}

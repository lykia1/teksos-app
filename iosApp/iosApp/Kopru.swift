import Foundation
import UIKit
import CoreLocation
import Shared
import OneSignalFramework

/// Ortak koddaki IosKopru arayüzünün iPhone karşılığı: bildirim, kurye konumu, AirPrint.
final class Kopru: NSObject, IosKopru, CLLocationManagerDelegate {
    static let ortak = Kopru()

    private let yonetici = CLLocationManager()
    private var izinSonuc: ((KotlinBoolean) -> Void)?
    private var calisiyor = false
    private var sonGonderim = Date.distantPast
    private let ayar = UserDefaults.standard

    override init() {
        super.init()
        yonetici.delegate = self
        yonetici.desiredAccuracy = kCLLocationAccuracyNearestTenMeters
        yonetici.distanceFilter = 15
        yonetici.activityType = .automotiveNavigation
        yonetici.pausesLocationUpdatesAutomatically = false
    }

    /// Uygulama kapanıp açıldıysa kurye konum paylaşımı kaldığı yerden sürer
    func kaldigiYerdenSurdur() {
        if ayar.bool(forKey: "konum_aktif") { _ = baslat() }
    }

    // MARK: - Sürüm

    func surum() -> String {
        let v = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "?"
        let b = Bundle.main.infoDictionary?["CFBundleVersion"] as? String ?? "?"
        return "\(v) (\(b))"
    }

    // MARK: - Bildirimler (OneSignal)

    func bildirimGiris(kimlik: String) { OneSignal.login(kimlik) }
    func bildirimCikis() { OneSignal.logout() }
    func bildirimIzniIste(ayaraGotur: Bool) {
        OneSignal.Notifications.requestPermission({ _ in }, fallbackToSettings: ayaraGotur)
    }
    func bildirimAcik() -> Bool { OneSignal.Notifications.permission }

    // MARK: - Kurye konumu

    func konumCalisiyor() -> Bool { calisiyor }

    func konumIzniVar() -> Bool {
        let d = yonetici.authorizationStatus
        return d == .authorizedAlways || d == .authorizedWhenInUse
    }

    func konumIzniIste(sonuc: @escaping (KotlinBoolean) -> Void) {
        switch yonetici.authorizationStatus {
        case .notDetermined:
            izinSonuc = sonuc
            yonetici.requestWhenInUseAuthorization()
        case .authorizedWhenInUse, .authorizedAlways:
            sonuc(KotlinBoolean(value: true))
        default:
            sonuc(KotlinBoolean(value: false))
        }
    }

    func locationManagerDidChangeAuthorization(_ m: CLLocationManager) {
        let d = m.authorizationStatus
        if d == .notDetermined { return }
        if let s = izinSonuc {
            izinSonuc = nil
            s(KotlinBoolean(value: konumIzniVar()))
        }
        // Telefon kilitliyken de konum gitsin diye "Her Zaman" izni bir kez istenir
        if d == .authorizedWhenInUse && !ayar.bool(forKey: "her_zaman_soruldu") {
            ayar.set(true, forKey: "her_zaman_soruldu")
            m.requestAlwaysAuthorization()
        }
    }

    func konumBaslat(url: String, kod: String) -> Bool {
        ayar.set(url, forKey: "konum_url")
        ayar.set(kod, forKey: "konum_kod")
        ayar.set(true, forKey: "konum_aktif")
        return baslat()
    }

    private func baslat() -> Bool {
        guard konumIzniVar() else { return false }
        yonetici.allowsBackgroundLocationUpdates = true
        yonetici.showsBackgroundLocationIndicator = true
        yonetici.startUpdatingLocation()
        calisiyor = true
        return true
    }

    func konumDurdur() {
        ayar.set(false, forKey: "konum_aktif")
        yonetici.stopUpdatingLocation()
        yonetici.allowsBackgroundLocationUpdates = false
        calisiyor = false
    }

    func locationManager(_ m: CLLocationManager, didUpdateLocations konumlar: [CLLocation]) {
        guard let k = konumlar.last,
              Date().timeIntervalSince(sonGonderim) >= 15,
              let adres = ayar.string(forKey: "konum_url"), let kod = ayar.string(forKey: "konum_kod"),
              let url = URL(string: adres) else { return }
        sonGonderim = Date()
        var istek = URLRequest(url: url)
        istek.httpMethod = "POST"
        istek.setValue("application/json", forHTTPHeaderField: "Content-Type")
        istek.setValue("TeksosApp/3.0 iOS", forHTTPHeaderField: "User-Agent")
        let govde: [String: Any] = [
            "device_id": kod,
            "location": ["coords": [
                "latitude": k.coordinate.latitude, "longitude": k.coordinate.longitude,
                "accuracy": k.horizontalAccuracy, "speed": max(0, k.speed),
            ]],
        ]
        istek.httpBody = try? JSONSerialization.data(withJSONObject: govde)
        URLSession.shared.dataTask(with: istek).resume()
    }

    // MARK: - Fiş yazdırma (AirPrint)

    func yazdir(html: String, ad: String) {
        DispatchQueue.main.async {
            let bilgi = UIPrintInfo(dictionary: nil)
            bilgi.outputType = .general
            bilgi.jobName = ad
            let y = UIPrintInteractionController.shared
            y.printInfo = bilgi
            y.printFormatter = UIMarkupTextPrintFormatter(markupText: html)
            y.present(animated: true, completionHandler: nil)
        }
    }
}

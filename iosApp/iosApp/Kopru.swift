import Foundation
import UIKit
import CoreLocation
import AVFoundation
import AudioToolbox
import Shared

/// Ortak koddaki IosKopru arayüzünün iPhone karşılığı: bildirim, kurye konumu, AirPrint.
final class Kopru: NSObject, IosKopru, CLLocationManagerDelegate {
    static let ortak = Kopru()

    private let yonetici = CLLocationManager()
    private var izinSonuc: ((KotlinBoolean) -> Void)?
    private var calisiyor = false
    private var sonGonderim = Date.distantPast
    private let ayar = UserDefaults.standard
    private let tekYonetici = CLLocationManager()
    private var tekSonuc: ((String?) -> Void)?
    private var tekIzinBekliyor = false
    private var fotoSonuc: ((String?) -> Void)?

    override init() {
        super.init()
        yonetici.delegate = self
        yonetici.desiredAccuracy = kCLLocationAccuracyNearestTenMeters
        yonetici.distanceFilter = 15
        yonetici.activityType = .automotiveNavigation
        yonetici.pausesLocationUpdatesAutomatically = false
        tekYonetici.delegate = self
        tekYonetici.desiredAccuracy = kCLLocationAccuracyBest
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

    // MARK: - Bildirimler (Bildirim.swift)

    func bildirimGiris(kimlik: String) { Bildirimler.ortak.giris(kimlik) }
    func bildirimCikis() { Bildirimler.ortak.cikis() }
    func bildirimIzniIste(ayaraGotur: Bool) { Bildirimler.ortak.izinIste(ayaraGotur: ayaraGotur) }
    func bildirimAcik() -> Bool { Bildirimler.ortak.izinVar }

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
        if m === tekYonetici {
            if tekIzinBekliyor {
                tekIzinBekliyor = false
                if konumIzniVar() { m.requestLocation() } else { tekBitir(nil) }
            }
            return
        }
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
        if m === tekYonetici {
            if let k = konumlar.last { tekBitir("\(k.coordinate.latitude),\(k.coordinate.longitude)") }
            return
        }
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

    func locationManager(_ m: CLLocationManager, didFailWithError error: Error) {
        if m === tekYonetici { tekBitir(nil) }
    }

    // MARK: - QR mesai: tek seferlik konum, QR okuma, fotoğraf

    private func tekBitir(_ s: String?) {
        DispatchQueue.main.async {
            guard let f = self.tekSonuc else { return }
            self.tekSonuc = nil
            f(s)
        }
    }

    func konumAl(sonuc: @escaping (String?) -> Void) {
        tekSonuc = sonuc
        DispatchQueue.main.asyncAfter(deadline: .now() + 20) { [weak self] in self?.tekBitir(nil) }
        switch tekYonetici.authorizationStatus {
        case .notDetermined:
            tekIzinBekliyor = true
            tekYonetici.requestWhenInUseAuthorization()
        case .authorizedWhenInUse, .authorizedAlways:
            tekYonetici.requestLocation()
        default:
            tekBitir(nil)
        }
    }

    private func ustEkran() -> UIViewController? {
        let pencere = UIApplication.shared.connectedScenes.compactMap { ($0 as? UIWindowScene)?.keyWindow }.first
        var vc = pencere?.rootViewController
        while let ust = vc?.presentedViewController { vc = ust }
        return vc
    }

    private func kameraIzni(_ devam: @escaping (Bool) -> Void) {
        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .authorized: devam(true)
        case .notDetermined:
            AVCaptureDevice.requestAccess(for: .video) { v in DispatchQueue.main.async { devam(v) } }
        default:
            let u = UIAlertController(title: "Kamera izni kapalı", message: "QR okutmak ve fotoğraf çekmek için Ayarlar'dan Teksos'a kamera izni verin.", preferredStyle: .alert)
            u.addAction(UIAlertAction(title: "Vazgeç", style: .cancel) { _ in devam(false) })
            u.addAction(UIAlertAction(title: "Ayarlar", style: .default) { _ in
                if let url = URL(string: UIApplication.openSettingsURLString) { UIApplication.shared.open(url) }
                devam(false)
            })
            ustEkran()?.present(u, animated: true)
        }
    }

    func qrOku(sonuc: @escaping (String?) -> Void) {
        DispatchQueue.main.async {
            self.kameraIzni { izin in
                guard izin, let ust = self.ustEkran() else { return sonuc(nil) }
                let vc = QrOkuyucu(sonuc: sonuc)
                vc.modalPresentationStyle = .fullScreen
                ust.present(vc, animated: true)
            }
        }
    }

    func fotoCek(onKamera: Bool, sonuc: @escaping (String?) -> Void) {
        DispatchQueue.main.async {
            guard UIImagePickerController.isSourceTypeAvailable(.camera) else { return sonuc(nil) }
            self.kameraIzni { izin in
                guard izin, let ust = self.ustEkran() else { return sonuc(nil) }
                let p = UIImagePickerController()
                p.sourceType = .camera
                if onKamera && UIImagePickerController.isCameraDeviceAvailable(.front) { p.cameraDevice = .front }
                p.delegate = self
                self.fotoSonuc = sonuc
                ust.present(p, animated: true)
            }
        }
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


// MARK: - Fotoğraf seçici

extension Kopru: UIImagePickerControllerDelegate, UINavigationControllerDelegate {
    func imagePickerController(_ p: UIImagePickerController, didFinishPickingMediaWithInfo bilgi: [UIImagePickerController.InfoKey: Any]) {
        let resim = bilgi[.originalImage] as? UIImage
        p.dismiss(animated: true) {
            let f = self.fotoSonuc
            self.fotoSonuc = nil
            f?(resim.flatMap { Kopru.kucult($0) })
        }
    }

    func imagePickerControllerDidCancel(_ p: UIImagePickerController) {
        p.dismiss(animated: true) {
            let f = self.fotoSonuc
            self.fotoSonuc = nil
            f?(nil)
        }
    }

    static func kucult(_ r: UIImage, enFazla: CGFloat = 1280) -> String? {
        let olcek = min(1, enFazla / max(r.size.width, r.size.height))
        let boyut = CGSize(width: r.size.width * olcek, height: r.size.height * olcek)
        let bicim = UIGraphicsImageRendererFormat.default()
        bicim.scale = 1
        let yeni = UIGraphicsImageRenderer(size: boyut, format: bicim).image { _ in r.draw(in: CGRect(origin: .zero, size: boyut)) }
        return yeni.jpegData(compressionQuality: 0.75)?.base64EncodedString()
    }
}

// MARK: - QR okuyucu ekranı

final class QrOkuyucu: UIViewController, AVCaptureMetadataOutputObjectsDelegate {
    private let oturum = AVCaptureSession()
    private var sonuc: ((String?) -> Void)?
    private var onizleme: AVCaptureVideoPreviewLayer?

    init(sonuc: @escaping (String?) -> Void) {
        self.sonuc = sonuc
        super.init(nibName: nil, bundle: nil)
    }

    required init?(coder: NSCoder) { fatalError() }

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .black
        guard let kamera = AVCaptureDevice.default(for: .video),
              let girdi = try? AVCaptureDeviceInput(device: kamera), oturum.canAddInput(girdi) else {
            DispatchQueue.main.async { self.bitir(nil) }
            return
        }
        oturum.addInput(girdi)
        let cikti = AVCaptureMetadataOutput()
        if oturum.canAddOutput(cikti) {
            oturum.addOutput(cikti)
            cikti.setMetadataObjectsDelegate(self, queue: .main)
            cikti.metadataObjectTypes = [.qr]
        }
        let k = AVCaptureVideoPreviewLayer(session: oturum)
        k.videoGravity = .resizeAspectFill
        view.layer.addSublayer(k)
        onizleme = k

        let cerceve = UIView()
        cerceve.layer.borderColor = UIColor(red: 0.97, green: 0.45, blue: 0.09, alpha: 1).cgColor
        cerceve.layer.borderWidth = 4
        cerceve.layer.cornerRadius = 20
        cerceve.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(cerceve)

        let yazi = UILabel()
        yazi.text = "Dükkandaki ekranın QR kodunu çerçeveye getirin"
        yazi.textColor = .white
        yazi.font = .boldSystemFont(ofSize: 17)
        yazi.numberOfLines = 0
        yazi.textAlignment = .center
        yazi.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(yazi)

        let vazgec = UIButton(type: .system)
        vazgec.setTitle("Vazgeç", for: .normal)
        vazgec.setTitleColor(.white, for: .normal)
        vazgec.titleLabel?.font = .boldSystemFont(ofSize: 18)
        vazgec.backgroundColor = UIColor(white: 0.15, alpha: 0.85)
        vazgec.layer.cornerRadius = 14
        vazgec.translatesAutoresizingMaskIntoConstraints = false
        vazgec.addTarget(self, action: #selector(vazgecildi), for: .touchUpInside)
        view.addSubview(vazgec)

        NSLayoutConstraint.activate([
            cerceve.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            cerceve.centerYAnchor.constraint(equalTo: view.centerYAnchor),
            cerceve.widthAnchor.constraint(equalTo: view.widthAnchor, multiplier: 0.7),
            cerceve.heightAnchor.constraint(equalTo: cerceve.widthAnchor),
            yazi.bottomAnchor.constraint(equalTo: cerceve.topAnchor, constant: -24),
            yazi.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 24),
            yazi.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -24),
            vazgec.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor, constant: -24),
            vazgec.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 24),
            vazgec.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -24),
            vazgec.heightAnchor.constraint(equalToConstant: 54),
        ])
        DispatchQueue.global(qos: .userInitiated).async { self.oturum.startRunning() }
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        onizleme?.frame = view.bounds
    }

    @objc private func vazgecildi() { bitir(nil) }

    func metadataOutput(_ o: AVCaptureMetadataOutput, didOutput nesneler: [AVMetadataObject], from baglanti: AVCaptureConnection) {
        guard let kod = (nesneler.first as? AVMetadataMachineReadableCodeObject)?.stringValue else { return }
        AudioServicesPlaySystemSound(kSystemSoundID_Vibrate)
        bitir(kod)
    }

    private func bitir(_ s: String?) {
        guard let f = sonuc else { return }
        sonuc = nil
        DispatchQueue.global(qos: .userInitiated).async { self.oturum.stopRunning() }
        dismiss(animated: true) { f(s) }
    }
}

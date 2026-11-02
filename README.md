# Teksos uygulaması — Android ve iPhone (tek kod)

Teksos Döner / Döner Atölyesi yönetim sisteminin telefon uygulaması. Bütün ekranlar
Kotlin + Compose Multiplatform ile **bir kez** yazılır; Android ve iPhone aynı kodu kullanır.
Veriler sunucudaki `app_api.php` (ve kasa için `kasa/api.php`) üzerinden gelir.

## Klasörler
- `shared/` — ortak kod
  - `commonMain/` — bütün ekranlar (`ui/`), sunucu bağlantısı (`Api.kt`), oturum, biçimler,
    JSON (`json/Json.kt`, Android'deki org.json ile aynı davranır)
  - `androidMain/`, `iosMain/` — telefona özgü küçük parçalar (saat, ağ motoru; iPhone kabuğu `IosPlatform.kt`)
  - `commonTest/` — JSON / sayı / tarih biçimi testleri
- `app/` — Android kabuğu: `MainActivity`, OneSignal, arka plan konum servisi, yazdırma, web görünümü (`AndroidPlatform.kt`)
- `iosApp/` — iPhone kabuğu (Swift): OneSignal, arka plan konumu, AirPrint (`Kopru.swift`).
  Xcode projesi `project.yml`'den XcodeGen ile üretilir.

Telefona özgü her şey `Platform` arayüzünden geçer (`shared/.../Platform.kt`); ekranlar yalnızca `os.…` çağırır.

## Derleme (GitHub Actions)
- `android.yml` — `main`'e her gönderimde APK: **Releases** ve `apk-son` dalı. Gizli ayar: `KS_PASS`.
- `iphone-yayin.yml` — `main`'e her gönderimde iPhone sürümü TestFlight'a yüklenir.
  Gizli ayarlar: `ASC_KEY_ID`, `ASC_ISSUER_ID`, `ASC_KEY_P8`, `APPLE_TEAM_ID` (yoksa atlanır).
- `ios.yml` — `ios` dalında deneme: Android derleme + testler + emülatör ekran görüntüsü (`android-sonuc` dalı),
  iPhone simülatör derlemesi + ekran görüntüsü (`ios-sonuc` dalı).

Sürüm adı `surum.txt`. Derleme hata verirse kayıt `derleme-log` dalındaki `son-hata.log` dosyasına yazılır.

## Mac'te elle derleme (isteğe bağlı)
```
gradle :shared:linkDebugFrameworkIosSimulatorArm64
brew install xcodegen && cd iosApp && xcodegen && open iosApp.xcodeproj
```

# Teksos Android uygulaması (yerli, Kotlin + Jetpack Compose)

Teksos Döner / Döner Atölyesi yönetim sisteminin Android uygulaması. Ekranlar telefonun
kendi diliyle yazılmıştır; veriler sunucudaki `app_api.php` üzerinden gelir.

## Yerli ekranlar (1. aşama)
- Giriş (şube seçimi, 2 adımlı doğrulama, tek cihaz kilidi)
- Özet (patron): tüm şubeler, bugünkü ciro, net kâr, hedef, son 7 gün, saatlik, kanal, ödeme, en çok satan, mesaidekiler
- Siparişler: tarih/durum/tür filtreleri, arama, detay (ürünler, ödemeler, müşteri, ara/yol tarifi)
- Kurye: bekleyen/üzerimdeki paketler, al/bırak/teslim + tahsilat, arama, yol tarifi, WhatsApp,
  arka planda konum (telefon kilitliyken de, `KonumServisi`)
- Profil: şube değiştirme, bildirim/pil/izin ayarları, çıkış
- Menü: henüz yerlisi olmayan sayfalar uygulama içinde (aynı oturumla) açılır

Bildirimler OneSignal ile (yeni paket → kuryeler; Telegram uyarıları → yöneticiler).

## Derleme
`main` dalına her gönderimde GitHub Actions APK'yı derler: **Releases** ve `apk-son` dalı.
Sürüm adı `surum.txt`, sürüm kodu `100 + derleme no`.
Gerekli gizli ayar: `KS_PASS` (imza dosyası `imza/teksos.jks.b64` şifresi).
Derleme hata verirse kayıt `derleme-log` dalındaki `son-hata.log` dosyasına yazılır.

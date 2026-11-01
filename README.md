# Teksos telefon uygulaması (Android)

Teksos Döner / Döner Atölyesi yönetim sisteminin Android uygulaması. Ekranlar siteden
(https://teksosdonerbesevler.com) gelir; uygulama telefonun yapabildiklerini ekler:

- Kurye konumu telefon kilitliyken de gönderilir (bildirim çubuğunda "Konumunuz paylaşılıyor").
- Bildirimler (OneSignal): yeni paket → kuryeler, Telegram uyarıları → yöneticiler.
- Giriş bir kez yapılır (180 gün hatırlanır). Yönetici → Patron ekranı, kurye → Kurye ekranı açılır.
- Excel/PDF indirmeleri İndirilenler klasörüne gider.

## Derleme

`main` dalına her gönderimde GitHub Actions APK'yı derler ve **Releases** bölümüne koyar.
Sürüm numarası `package.json` → `version`.

Gerekli gizli ayar (bir kez): Settings → Secrets and variables → Actions → `KS_PASS`
(imza dosyası `imza/teksos.jks.b64` şifresi). İmza dosyası değişirse eski kurulumların
üstüne güncelleme yüklenemez — dosyayı ve şifreyi saklayın.

Derleme hata verirse kayıt `derleme-log` dalındaki `son-hata.log` dosyasına yazılır.

## Dosyalar

- `capacitor.config.json` — site adresi, uygulama adı
- `native/` — Android kodu (bildirim, konum servisi, köprü eklentisi)
- `scripts/hazirla.js` — Capacitor şablonunu Teksos'a göre düzenler
- `ikonlar.json` — uygulama ve bildirim simgeleri

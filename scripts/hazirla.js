// "npx cap add android" sonrası Android projesini Teksos'a göre düzenler.
// Kullanım: node scripts/hazirla.js <versionCode> <versionName>
const fs = require('fs');
const path = require('path');

const kok = path.resolve(__dirname, '..');
const app = path.join(kok, 'android', 'app');
const ana = path.join(app, 'src', 'main');
const [, , vKod = '1', vAd = '1.0.0'] = process.argv;

function degistir(dosya, eski, yeni, zorunlu = true) {
  let s = fs.readFileSync(dosya, 'utf8');
  if (typeof eski === 'string' ? !s.includes(eski) : !eski.test(s)) {
    if (zorunlu) throw new Error(`${path.basename(dosya)}: "${eski}" bulunamadı`);
    return;
  }
  s = s.replace(eski, yeni);
  fs.writeFileSync(dosya, s);
}

// 1) Java dosyaları
const pkg = path.join(ana, 'java', 'com', 'teksosdoner', 'app');
fs.mkdirSync(pkg, { recursive: true });
for (const f of fs.readdirSync(path.join(kok, 'native'))) {
  fs.copyFileSync(path.join(kok, 'native', f), path.join(pkg, f));
}

// 2) İzinler, servis, uygulama sınıfı
const man = path.join(ana, 'AndroidManifest.xml');
degistir(man, '<application', '<application\n        android:name=".Uygulama"');
degistir(man, '</application>', `    <service
            android:name=".KonumServisi"
            android:exported="false"
            android:foregroundServiceType="location" />
    </application>`);
const izinler = ['ACCESS_FINE_LOCATION', 'ACCESS_COARSE_LOCATION', 'FOREGROUND_SERVICE', 'FOREGROUND_SERVICE_LOCATION',
  'POST_NOTIFICATIONS', 'WAKE_LOCK', 'REQUEST_IGNORE_BATTERY_OPTIMIZATIONS', 'CAMERA', 'VIBRATE']
  .map((i) => `    <uses-permission android:name="android.permission.${i}" />`).join('\n');
degistir(man, '</manifest>', izinler + '\n</manifest>');

// 3) Bağımlılıklar, sürüm, imzalama
const gradle = path.join(app, 'build.gradle');
degistir(gradle, /versionCode \d+/, `versionCode ${parseInt(vKod, 10)}`);
degistir(gradle, /versionName "[^"]*"/, `versionName "${vAd}"`);
degistir(gradle, /dependencies \{/, `dependencies {
    implementation 'com.google.android.gms:play-services-location:21.3.0'
    implementation 'com.onesignal:OneSignal:[5.1.6, 5.99.99]'`);
degistir(gradle, /buildTypes \{/, `signingConfigs {
        release {
            storeFile file(System.getenv("KS_FILE") ?: "teksos.jks")
            storePassword System.getenv("KS_PASS")
            keyAlias "teksos"
            keyPassword System.getenv("KS_PASS")
        }
    }
    buildTypes {`);
degistir(gradle, /release \{\s*\n(\s*)minifyEnabled/, (m, bosluk) => `release {\n${bosluk}signingConfig signingConfigs.release\n${bosluk}minifyEnabled`);

// 4) Simgeler
const ikonlar = JSON.parse(fs.readFileSync(path.join(kok, 'ikonlar.json'), 'utf8'));
for (const [yol, b64] of Object.entries(ikonlar)) {
  const hedef = path.join(ana, 'res', yol);
  fs.mkdirSync(path.dirname(hedef), { recursive: true });
  fs.writeFileSync(hedef, Buffer.from(b64, 'base64'));
}
// Uyarlanabilir (adaptive) şablon simgesi yerine bizim simge kullanılsın
fs.rmSync(path.join(ana, 'res', 'mipmap-anydpi-v26'), { recursive: true, force: true });
for (const d of fs.readdirSync(path.join(ana, 'res'))) {
  if (d.startsWith('mipmap-')) fs.rmSync(path.join(ana, 'res', d, 'ic_launcher_foreground.png'), { force: true });
}

// 5) Uygulama adı
const str = path.join(ana, 'res', 'values', 'strings.xml');
degistir(str, /<string name="app_name">[^<]*<\/string>/, '<string name="app_name">Teksos</string>');
degistir(str, /<string name="title_activity_main">[^<]*<\/string>/, '<string name="title_activity_main">Teksos</string>', false);

console.log('Android projesi hazır:', vAd, '(' + vKod + ')');

#!/usr/bin/env python3
"""
iPhone dağıtım imzası (App Store / TestFlight) — GitHub Actions'ta çalışır.

1) Apple Distribution sertifikası: ilk çalıştırmada App Store Connect API ile oluşturulur;
   özel anahtarıyla birlikte .p12 olarak şifrelenip deponun `imza-ios` dalında saklanır
   (şifre, ASC_KEY_P8 gizli ayarından türetilir; dal tek başına işe yaramaz).
   Sonraki çalıştırmalar aynı sertifikayı kullanır.
2) App Store dağıtım profili (com.teksosdoner.app): yoksa oluşturulur, her seferinde indirilir.

Çıktı (çalışma klasörüne): imza/dagitim.p12, imza/profil.mobileprovision, imza/ortam.sh
"""
import base64, hashlib, json, os, subprocess, sys, time, urllib.error, urllib.request

import jwt  # pyjwt

KID = os.environ["ASC_KEY_ID"]
ISS = os.environ["ASC_ISSUER_ID"]
P8 = os.environ["ASC_KEY_P8"]
TEAM = os.environ["APPLE_TEAM_ID"]
PAKET = "com.teksosdoner.app"
PROFIL_ADI = "Teksos App Store (GitHub)"
SIFRE = hashlib.sha256((P8.strip() + "|teksos-imza").encode()).hexdigest()[:40]
KLASOR = "imza"
os.makedirs(KLASOR, exist_ok=True)


def jeton():
    return jwt.encode({"iss": ISS, "iat": int(time.time()), "exp": int(time.time()) + 900, "aud": "appstoreconnect-v1"},
                      P8, algorithm="ES256", headers={"kid": KID})


def api(yol, veri=None, yontem=None):
    istek = urllib.request.Request("https://api.appstoreconnect.apple.com" + yol,
                                   data=json.dumps(veri).encode() if veri is not None else None,
                                   headers={"Authorization": "Bearer " + jeton(), "Content-Type": "application/json"},
                                   method=yontem or ("POST" if veri is not None else "GET"))
    try:
        with urllib.request.urlopen(istek) as y:
            govde = y.read()
            return json.loads(govde) if govde else {}
    except urllib.error.HTTPError as e:
        print("Apple API hatası", e.code, yol, e.read().decode()[:600])
        raise


def kos(*komut, girdi=None):
    return subprocess.run(komut, check=True, capture_output=True, input=girdi).stdout


def sakli_sertifika():
    """imza-ios dalındaki şifreli .p12 (varsa ve Apple'da hâlâ geçerliyse)"""
    try:
        subprocess.run(["git", "fetch", "-q", "origin", "imza-ios"], check=True, capture_output=True)
        sifreli = kos("git", "show", "FETCH_HEAD:dagitim.p12.enc")
        kimlik = kos("git", "show", "FETCH_HEAD:sertifika_id.txt").decode().strip()
    except subprocess.CalledProcessError:
        return None
    try:
        s = api(f"/v1/certificates/{kimlik}")["data"]
        if s["attributes"].get("expirationDate", "9999") < time.strftime("%Y-%m-%dT%H:%M:%S"):
            print("Saklı sertifikanın süresi dolmuş, yenisi oluşturulacak.")
            return None
    except urllib.error.HTTPError:
        print("Saklı sertifika Apple'da bulunamadı, yenisi oluşturulacak.")
        return None
    with open(f"{KLASOR}/dagitim.p12.enc", "wb") as f:
        f.write(sifreli)
    kos("openssl", "enc", "-d", "-aes-256-cbc", "-pbkdf2", "-pass", "pass:" + SIFRE,
        "-in", f"{KLASOR}/dagitim.p12.enc", "-out", f"{KLASOR}/dagitim.p12")
    print("Saklı dağıtım sertifikası kullanılıyor:", kimlik)
    return kimlik


def yeni_sertifika():
    kos("openssl", "genrsa", "-out", f"{KLASOR}/anahtar.pem", "2048")
    kos("openssl", "req", "-new", "-key", f"{KLASOR}/anahtar.pem", "-out", f"{KLASOR}/istek.csr",
        "-subj", f"/emailAddress=derleme@teksosdoner.app/CN=Teksos GitHub/C=TR")
    csr = open(f"{KLASOR}/istek.csr").read()
    s = api("/v1/certificates", {"data": {"type": "certificates",
                                          "attributes": {"certificateType": "DISTRIBUTION", "csrContent": csr}}})["data"]
    kimlik = s["id"]
    with open(f"{KLASOR}/sertifika.cer", "wb") as f:
        f.write(base64.b64decode(s["attributes"]["certificateContent"]))
    kos("openssl", "x509", "-inform", "DER", "-in", f"{KLASOR}/sertifika.cer", "-out", f"{KLASOR}/sertifika.pem")
    kos("openssl", "pkcs12", "-export", "-legacy", "-inkey", f"{KLASOR}/anahtar.pem", "-in", f"{KLASOR}/sertifika.pem",
        "-name", "Teksos Dagitim", "-out", f"{KLASOR}/dagitim.p12", "-passout", "pass:" + SIFRE)
    kos("openssl", "enc", "-aes-256-cbc", "-pbkdf2", "-salt", "-pass", "pass:" + SIFRE,
        "-in", f"{KLASOR}/dagitim.p12", "-out", f"{KLASOR}/dagitim.p12.enc")
    # Şifreli sertifikayı imza-ios dalına yaz (sonraki derlemeler aynısını kullansın)
    calisma = "/tmp/imza-dal"
    subprocess.run(["rm", "-rf", calisma], check=True)
    os.makedirs(calisma)
    kos("cp", f"{KLASOR}/dagitim.p12.enc", calisma + "/dagitim.p12.enc")
    with open(calisma + "/sertifika_id.txt", "w") as f:
        f.write(kimlik + "\n")
    with open(calisma + "/BENIOKU.txt", "w") as f:
        f.write("iPhone dağıtım sertifikası (şifreli). Şifre ASC_KEY_P8 gizli ayarından türetilir; iosApp/imza.py kullanır.\n")
    depo = os.environ["DEPO_URL"]
    for k in (["git", "init", "-q"], ["git", "checkout", "-q", "-b", "imza-ios"], ["git", "add", "-A"],
              ["git", "-c", "user.name=derleme", "-c", "user.email=derleme@users.noreply.github.com", "commit", "-qm", "iPhone dağıtım sertifikası"],
              ["git", "push", "-qf", depo, "imza-ios"]):
        subprocess.run(k, check=True, cwd=calisma, capture_output=True)
    print("Yeni dağıtım sertifikası oluşturuldu ve saklandı:", kimlik)
    return kimlik


def profil(sertifika_id):
    paket = api(f"/v1/bundleIds?filter[identifier]={PAKET}")["data"]
    paket = [p for p in paket if p["attributes"]["identifier"] == PAKET]
    if not paket:
        sys.exit(f"{PAKET} paket kimliği Apple hesabında bulunamadı.")
    paket_id = paket[0]["id"]
    for p in api(f"/v1/profiles?filter[name]={urllib.request.quote(PROFIL_ADI)}&include=certificates&limit=20").get("data", []):
        if p["attributes"]["profileState"] == "ACTIVE":
            sertler = [c["id"] for c in p.get("relationships", {}).get("certificates", {}).get("data", [])]
            if sertifika_id in sertler:
                return p["attributes"]["profileContent"], p["attributes"]["uuid"]
        # Eski / geçersiz profil silinir
        try:
            api(f"/v1/profiles/{p['id']}", yontem="DELETE")
        except urllib.error.HTTPError:
            pass
    p = api("/v1/profiles", {"data": {"type": "profiles", "attributes": {"name": PROFIL_ADI, "profileType": "IOS_APP_STORE"},
                                      "relationships": {"bundleId": {"data": {"type": "bundleIds", "id": paket_id}},
                                                        "certificates": {"data": [{"type": "certificates", "id": sertifika_id}]}}}})["data"]
    print("App Store profili oluşturuldu.")
    return p["attributes"]["profileContent"], p["attributes"]["uuid"]


kimlik = sakli_sertifika() or yeni_sertifika()
icerik, uuid = profil(kimlik)
with open(f"{KLASOR}/profil.mobileprovision", "wb") as f:
    f.write(base64.b64decode(icerik))
with open(f"{KLASOR}/ortam.sh", "w") as f:
    f.write(f"export P12_SIFRE='{SIFRE}'\nexport PROFIL_UUID='{uuid}'\n")
print("İmza hazır. Profil:", uuid)

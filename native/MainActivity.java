package com.teksosdoner.app;

import android.app.DownloadManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.webkit.WebView;
import android.widget.Toast;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    static final String SUNUCU = "https://teksosdonerbesevler.com/";
    private static volatile MainActivity aktif;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(TeksosPlugin.class);
        super.onCreate(savedInstanceState);
        aktif = this;
        WebView w = getBridge().getWebView();
        // Excel / PDF gibi dosyalar: oturum çereziyle İndirilenler klasörüne
        w.setDownloadListener((url, ua, icerik, mime, uzunluk) -> {
            try {
                DownloadManager.Request r = new DownloadManager.Request(Uri.parse(url));
                String cerez = CookieManager.getInstance().getCookie(url);
                if (cerez != null) r.addRequestHeader("Cookie", cerez);
                r.addRequestHeader("User-Agent", ua);
                String ad = URLUtil.guessFileName(url, icerik, mime);
                r.setTitle(ad);
                r.setMimeType(mime);
                r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                r.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, ad);
                ((DownloadManager) getSystemService(DOWNLOAD_SERVICE)).enqueue(r);
                Toast.makeText(this, "İndiriliyor: " + ad, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Dosya indirilemedi", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        aktif = this;
        urlAc();
    }

    @Override
    public void onDestroy() {
        if (aktif == this) aktif = null;
        super.onDestroy();
    }

    /** Bildirime dokunulduysa ilgili sayfayı (ör. kurye ekranı) açar. Yalnızca kendi sitemizin adresleri. */
    static void urlAc() {
        final MainActivity a = aktif;
        final String u = Uygulama.bekleyenUrl;
        if (a == null || u == null || a.getBridge() == null) return;
        Uygulama.bekleyenUrl = null;
        final String tam = u.startsWith("http") ? u : SUNUCU + u.replaceFirst("^/+", "");
        if (!tam.startsWith(SUNUCU)) return;
        a.runOnUiThread(() -> a.getBridge().getWebView().loadUrl(tam));
    }
}

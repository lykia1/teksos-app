package com.teksosdoner.app.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.teksosdoner.app.Api
import com.teksosdoner.app.Oturum

private const val CEREZ = "teksos_app"

/** Henüz yerli ekranı olmayan sayfalar: uygulamanın içinde, aynı oturumla açılır. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebEkrani(url: String, baslik: String, geri: (() -> Unit)?) {
    val ctx = LocalContext.current
    var yukleniyor by remember { mutableStateOf(true) }
    var sayfaBaslik by remember { mutableStateOf(baslik) }
    var geriVar by remember { mutableStateOf(false) }
    var hata by remember { mutableStateOf<String?>(null) }
    var web by remember { mutableStateOf<WebView?>(null) }
    var dosyaCb by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    var izinIstek by remember { mutableStateOf<PermissionRequest?>(null) }

    val dosyaSec = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { s ->
        dosyaCb?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(s.resultCode, s.data))
        dosyaCb = null
    }
    val kameraIzni = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        izinIstek?.let { if (ok) it.grant(it.resources) else it.deny() }
        izinIstek = null
    }
    val konumIzni = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    val tamUrl = remember(url) {
        val s = Oturum.sube
        if (s.isNullOrEmpty()) url else url + (if (url.contains('?')) "&" else "?") + "_sube=" + s
    }

    BackHandler(enabled = geriVar || geri != null) {
        val w = web
        if (w != null && w.canGoBack()) w.goBack() else geri?.invoke()
    }

    Ekran(sayfaBaslik, geri = geri, eylemler = {
        IconButton(onClick = { hata = null; web?.reload() }) { Icon(Icons.Filled.Refresh, "Yenile") }
    }) {
        Column(Modifier.fillMaxSize()) {
            if (yukleniyor) LinearProgressIndicator(Modifier.fillMaxWidth().height(3.dp), color = Turuncu, trackColor = Color.Transparent)
            Box(Modifier.fillMaxSize()) {
                AndroidView(factory = { c ->
                    CookieManager.getInstance().apply {
                        setAcceptCookie(true)
                        Oturum.anahtar?.let { setCookie(Api.sunucu, "$CEREZ=$it; Path=/; Secure") }
                        flush()
                    }
                    WebView(c).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        settings.setGeolocationEnabled(true)
                        settings.userAgentString = settings.userAgentString + " " + Api.UA
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                val u = request.url
                                val bizim = (u.scheme == "https" || u.scheme == "http") && u.host == Uri.parse(Api.sunucu).host
                                if (bizim) {
                                    if (u.path?.endsWith("logout.php") == true) { Oturum.cikis(); return true }
                                    return false
                                }
                                runCatching { c.startActivity(Intent(Intent.ACTION_VIEW, u)) }
                                return true
                            }
                            override fun onPageStarted(view: WebView, u: String?, favicon: android.graphics.Bitmap?) { yukleniyor = true }
                            override fun onPageFinished(view: WebView, u: String?) {
                                yukleniyor = false
                                view.title?.substringBefore(" · ")?.takeIf { it.isNotBlank() && !it.startsWith("http") }?.let { sayfaBaslik = it }
                                if (u?.contains("login.php") == true) Oturum.cikis()
                            }
                            override fun doUpdateVisitedHistory(view: WebView, u: String?, isReload: Boolean) { geriVar = view.canGoBack() }
                            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                                if (request.isForMainFrame) hata = "Sayfa açılamadı. İnternet bağlantısını kontrol edin."
                            }
                        }
                        webChromeClient = object : WebChromeClient() {
                            override fun onShowFileChooser(w: WebView, cb: ValueCallback<Array<Uri>>, p: FileChooserParams): Boolean {
                                dosyaCb?.onReceiveValue(null)
                                dosyaCb = cb
                                return try { dosyaSec.launch(p.createIntent()); true } catch (e: Exception) { dosyaCb = null; false }
                            }
                            override fun onPermissionRequest(request: PermissionRequest) {
                                if (ContextCompat.checkSelfPermission(c, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) request.grant(request.resources)
                                else { izinIstek = request; kameraIzni.launch(Manifest.permission.CAMERA) }
                            }
                            override fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback) {
                                callback.invoke(origin, true, false)
                                if (ContextCompat.checkSelfPermission(c, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                    konumIzni.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                                }
                            }
                        }
                        setDownloadListener { dUrl, ua, icerik, mime, _ -> indir(c, dUrl, ua, icerik, mime) }
                        loadUrl(tamUrl)
                        web = this
                    }
                }, modifier = Modifier.fillMaxSize())
                if (hata != null) HataKutusu(hata!!) { hata = null; web?.reload() }
            }
        }
    }
}

private fun indir(c: Context, url: String, ua: String, icerik: String?, mime: String?) {
    try {
        val r = DownloadManager.Request(Uri.parse(url))
        CookieManager.getInstance().getCookie(url)?.let { r.addRequestHeader("Cookie", it) }
        r.addRequestHeader("User-Agent", ua)
        val ad = URLUtil.guessFileName(url, icerik, mime)
        r.setTitle(ad)
        r.setMimeType(mime)
        r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        r.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, ad)
        (c.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(r)
        Toast.makeText(c, "İndiriliyor: $ad", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(c, "Dosya indirilemedi", Toast.LENGTH_SHORT).show()
    }
}

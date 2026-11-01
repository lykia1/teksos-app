package com.teksosdoner.app

import android.annotation.SuppressLint
import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient

/** Adisyon / mutfak fişini telefonun yazdırma servisiyle basar (Wi-Fi / Bluetooth yazıcı, PDF). */
object Yazdir {
    private var tutulan: WebView? = null

    @SuppressLint("SetJavaScriptEnabled")
    fun fis(ctx: Context, siparisId: Int, mutfak: Boolean = false) {
        val ad = (if (mutfak) "Mutfak fişi #" else "Adisyon #") + siparisId
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            Oturum.anahtar?.let { setCookie(Api.sunucu, "teksos_app=$it; Path=/; Secure") }
        }
        val w = WebView(ctx)
        tutulan = w
        w.settings.javaScriptEnabled = true
        w.settings.userAgentString = w.settings.userAgentString + " " + Api.UA
        w.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String?) {
                val pm = ctx.getSystemService(Context.PRINT_SERVICE) as PrintManager
                pm.print(ad, view.createPrintDocumentAdapter(ad), PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A6).build())
            }
        }
        val sube = Oturum.sube?.let { "&_sube=$it" } ?: ""
        w.loadUrl(Api.sunucu + "kasa/fis.php?id=" + siparisId + (if (mutfak) "&kitchen=1" else "") + sube)
    }
}

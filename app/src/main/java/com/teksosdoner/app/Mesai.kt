package com.teksosdoner.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

/** QR mesai için yerli yardımcılar: QR okuma, fotoğraf çekme, tek seferlik konum. */
object Mesai {
    private val sayac = AtomicInteger()
    private val ana = Handler(Looper.getMainLooper())

    /** Ekran dışından tek seferlik sonuç başlatıcı (iş bitince kendini kaldırır) */
    private fun <I, O> tekSefer(a: ComponentActivity, sozlesme: ActivityResultContract<I, O>, girdi: I, sonuc: (O) -> Unit) {
        var b: ActivityResultLauncher<I>? = null
        b = a.activityResultRegistry.register("mesai_" + sayac.incrementAndGet(), sozlesme) { o ->
            b?.unregister()
            sonuc(o)
        }
        b.launch(girdi)
    }

    private fun izinIste(a: ComponentActivity, izinler: Array<String>, sonuc: (Boolean) -> Unit) {
        if (izinler.any { ContextCompat.checkSelfPermission(a, it) == PackageManager.PERMISSION_GRANTED }) return sonuc(true)
        tekSefer(a, ActivityResultContracts.RequestMultiplePermissions(), izinler) { m -> sonuc(m.values.any { it }) }
    }

    fun qrOku(c: Context, sonuc: (String?) -> Unit) {
        val secenek = GmsBarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).enableAutoZoom().build()
        GmsBarcodeScanning.getClient(c, secenek).startScan()
            .addOnSuccessListener { sonuc(it.rawValue) }
            .addOnCanceledListener { sonuc(null) }
            .addOnFailureListener { sonuc(null) }
    }

    @SuppressLint("MissingPermission")
    fun konumAl(a: ComponentActivity, sonuc: (String?) -> Unit) {
        izinIste(a, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) { izin ->
            if (!izin) return@izinIste sonuc(null)
            val istemci = LocationServices.getFusedLocationProviderClient(a)
            val iptal = CancellationTokenSource()
            var bitti = false
            fun bitir(s: String?) { if (!bitti) { bitti = true; sonuc(s) } }
            // 15 sn'de yeni konum gelmezse son bilinen konum
            ana.postDelayed({
                if (!bitti) {
                    iptal.cancel()
                    istemci.lastLocation.addOnCompleteListener { t -> bitir(t.result?.let { "${it.latitude},${it.longitude}" }) }
                }
            }, 15000)
            istemci.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, iptal.token)
                .addOnSuccessListener { k ->
                    if (k != null) bitir("${k.latitude},${k.longitude}")
                    else istemci.lastLocation.addOnCompleteListener { t -> bitir(t.result?.let { "${it.latitude},${it.longitude}" }) }
                }
                .addOnFailureListener { if (!iptal.token.isCancellationRequested) bitir(null) }
        }
    }

    /** Kamera fotoğrafı (onKamera: selfie) → küçültülmüş JPEG base64 */
    private class FotoSozlesme(private val onKamera: Boolean) : ActivityResultContracts.TakePicture() {
        override fun createIntent(context: Context, input: Uri): Intent = super.createIntent(context, input).apply {
            if (onKamera) {
                putExtra("android.intent.extras.CAMERA_FACING", 1)
                putExtra("android.intent.extras.LENS_FACING_FRONT", 1)
                putExtra("android.intent.extra.USE_FRONT_CAMERA", true)
            }
        }
    }

    fun fotoCek(a: ComponentActivity, onKamera: Boolean, sonuc: (String?) -> Unit) {
        izinIste(a, arrayOf(Manifest.permission.CAMERA)) { izin ->
            if (!izin) return@izinIste sonuc(null)
            val klasor = File(a.cacheDir, "foto").apply { mkdirs() }
            val dosya = File(klasor, "cekim_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(a, a.packageName + ".dosyalar", dosya)
            try {
                tekSefer(a, FotoSozlesme(onKamera), uri) { tamam ->
                    val b64 = if (tamam && dosya.length() > 0) runCatching { kucult(dosya) }.getOrNull() else null
                    dosya.delete()
                    sonuc(b64)
                }
            } catch (e: Exception) {
                sonuc(null)
            }
        }
    }

    private fun kucult(dosya: File, enFazla: Int = 1280): String? {
        val sinir = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(dosya.path, sinir)
        var oran = 1
        while (maxOf(sinir.outWidth, sinir.outHeight) / oran > enFazla * 2) oran *= 2
        var bmp = BitmapFactory.decodeFile(dosya.path, BitmapFactory.Options().apply { inSampleSize = oran }) ?: return null
        val donus = when (runCatching { ExifInterface(dosya.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, 1) }.getOrDefault(1)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        val olcek = minOf(1f, enFazla.toFloat() / maxOf(bmp.width, bmp.height))
        if (donus != 0f || olcek < 1f) {
            val m = Matrix().apply { postScale(olcek, olcek); postRotate(donus) }
            bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
        }
        val cikti = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 75, cikti)
        return Base64.encodeToString(cikti.toByteArray(), Base64.NO_WRAP)
    }
}

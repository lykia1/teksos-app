package com.teksosdoner.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat

/** Kurye konumu (telefon kilitliyken de) — KonumServisi'ni açar/kapatır. */
object Konum {
    fun calisiyor(): Boolean = KonumServisi.calisiyor

    fun izinVar(c: Context): Boolean =
        ContextCompat.checkSelfPermission(c, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(c, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    fun baslat(c: Context, url: String, kod: String): Boolean {
        if (!izinVar(c)) return false
        KonumServisi.ayarla(c, url, kod, 20, true)
        return try {
            ContextCompat.startForegroundService(c, Intent(c, KonumServisi::class.java))
            true
        } catch (e: Exception) {
            false
        }
    }

    fun durdur(c: Context) {
        KonumServisi.ayarla(c, null, null, 0, false)
        c.stopService(Intent(c, KonumServisi::class.java))
    }

    fun pilSerbest(c: Context): Boolean {
        val pm = c.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return pm?.isIgnoringBatteryOptimizations(c.packageName) ?: true
    }

    fun pilIzniIste(c: Context) {
        if (pilSerbest(c)) return
        runCatching {
            c.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + c.packageName))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}

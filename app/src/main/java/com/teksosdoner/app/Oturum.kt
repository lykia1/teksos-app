package com.teksosdoner.app

import android.content.Context
import android.content.SharedPreferences
import android.webkit.CookieManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
import java.util.UUID

/** Giriş anahtarı, kullanıcı bilgisi ve seçili şube (telefonda saklanır). */
object Oturum {
    private lateinit var p: SharedPreferences
    private val _girisli = MutableStateFlow(false)
    val girisli: StateFlow<Boolean> = _girisli
    private val _ben = MutableStateFlow<JSONObject?>(null)
    val ben: StateFlow<JSONObject?> = _ben

    fun baslat(c: Context) {
        p = c.getSharedPreferences("teksos", Context.MODE_PRIVATE)
        _ben.value = p.getString("ben", null)?.let { runCatching { JSONObject(it) }.getOrNull() }
        _girisli.value = anahtar != null && _ben.value != null
    }

    var anahtar: String?
        get() = p.getString("anahtar", null)
        private set(v) {
            p.edit().putString("anahtar", v).apply()
        }

    /** Yöneticinin seçtiği şube (boşsa giriş yapılan şube). */
    var sube: String?
        get() = p.getString("sube", null)
        set(v) {
            p.edit().putString("sube", v).apply()
        }

    /** Bu telefona özel kimlik (personel hesabı tek telefona kilitlidir). */
    val cihaz: String
        get() {
            p.getString("cihaz", null)?.let { return it }
            val yeni = UUID.randomUUID().toString().replace("-", "")
            p.edit().putString("cihaz", yeni).apply()
            return yeni
        }

    var konumKapatildi: Long
        get() = p.getLong("konum_kapatildi", 0L)
        set(v) {
            p.edit().putLong("konum_kapatildi", v).apply()
        }

    var pilSoruldu: Boolean
        get() = p.getBoolean("pil_soruldu", false)
        set(v) {
            p.edit().putBoolean("pil_soruldu", v).apply()
        }

    fun girisYap(yeniAnahtar: String, ben: JSONObject) {
        anahtar = yeniAnahtar
        benGuncelle(ben)
        _girisli.value = true
    }

    fun benGuncelle(b: JSONObject) {
        p.edit().putString("ben", b.toString()).apply()
        _ben.value = b
    }

    fun cikis() {
        p.edit().remove("anahtar").remove("ben").remove("sube").apply()
        runCatching { CookieManager.getInstance().removeAllCookies(null) }
        _ben.value = null
        _girisli.value = false
    }
}

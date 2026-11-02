package com.teksosdoner.app

import com.teksosdoner.app.json.JSONObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Giriş anahtarı, kullanıcı bilgisi ve seçili şube (telefonda saklanır). */
object Oturum {
    private val p: Depo get() = os.depo
    private val _girisli = MutableStateFlow(false)
    val girisli: StateFlow<Boolean> = _girisli
    private val _ben = MutableStateFlow<JSONObject?>(null)
    val ben: StateFlow<JSONObject?> = _ben

    fun baslat() {
        _ben.value = p.metin("ben")?.let { runCatching { JSONObject(it) }.getOrNull() }
        _girisli.value = anahtar != null && _ben.value != null
    }

    var anahtar: String?
        get() = p.metin("anahtar")
        private set(v) = p.metinYaz("anahtar", v)

    /** Yöneticinin seçtiği şube (boşsa giriş yapılan şube). */
    var sube: String?
        get() = p.metin("sube")
        set(v) = p.metinYaz("sube", v)

    /** Bu telefona özel kimlik (personel hesabı tek telefona kilitlidir). */
    val cihaz: String
        get() {
            p.metin("cihaz")?.let { return it }
            val yeni = rastgeleKimlik()
            p.metinYaz("cihaz", yeni)
            return yeni
        }

    var konumKapatildi: Long
        get() = p.sayi("konum_kapatildi")
        set(v) = p.sayiYaz("konum_kapatildi", v)

    var pilSoruldu: Boolean
        get() = p.sayi("pil_soruldu") == 1L
        set(v) = p.sayiYaz("pil_soruldu", if (v) 1L else 0L)

    fun girisYap(yeniAnahtar: String, ben: JSONObject) {
        anahtar = yeniAnahtar
        benGuncelle(ben)
        _girisli.value = true
    }

    fun benGuncelle(b: JSONObject) {
        p.metinYaz("ben", b.toString())
        _ben.value = b
    }

    fun cikis() {
        p.metinYaz("anahtar", null); p.metinYaz("ben", null); p.metinYaz("sube", null)
        runCatching { os.cerezTemizle() }
        _ben.value = null
        _girisli.value = false
    }
}

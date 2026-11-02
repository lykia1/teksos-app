package com.teksosdoner.app.json

/*
 * Android'deki org.json ile aynı davranan küçük JSON sınıfları (iPhone'da da çalışsın diye).
 * Davranış Android'inkiyle aynıdır: optString boş değerde "null" döner, optDouble yoksa NaN,
 * sayı yazılmış metinler optInt/optDouble ile sayıya çevrilir.
 */

class JSONException(mesaj: String) : RuntimeException(mesaj)

private object Bos {
    override fun toString() = "null"
    override fun equals(other: Any?) = other == null || other === this
    override fun hashCode() = 0
}

private fun sayiyaCevir(v: Any?): Double? = when (v) {
    is Number -> v.toDouble()
    is String -> v.trim().toDoubleOrNull()
    else -> null
}

private fun tamaCevir(v: Any?): Long? = when (v) {
    is Int -> v.toLong()
    is Long -> v
    is Number -> v.toDouble().let { if (it.isNaN()) null else it.toLong() }
    is String -> v.trim().let { s -> s.toLongOrNull() ?: s.toDoubleOrNull()?.toLong() }
    else -> null
}

private fun mantigaCevir(v: Any?): Boolean? = when (v) {
    is Boolean -> v
    is String -> when (v.lowercase()) { "true" -> true; "false" -> false; else -> null }
    else -> null
}

private fun metneCevir(v: Any?): String? = when (v) {
    null -> null
    is String -> v
    is Double -> if (v == kotlin.math.floor(v) && !v.isInfinite() && kotlin.math.abs(v) < 1e15) v.toString() else v.toString()
    else -> v.toString()
}

/** Gelen değeri JSON'a uygun hâle getirir (Kotlin listeleri/haritaları da kabul edilir). */
internal fun sar(v: Any?): Any? = when (v) {
    null -> null
    JSONObject.NULL -> JSONObject.NULL
    is JSONObject, is JSONArray, is String, is Boolean -> v
    is Int, is Long -> v
    is Number -> v.toDouble()
    is Char -> v.toString()
    is Map<*, *> -> JSONObject().also { o -> v.forEach { (k, d) -> o.put(k.toString(), d) } }
    is Collection<*> -> JSONArray(v)
    is Array<*> -> JSONArray(v.toList())
    else -> v.toString()
}

class JSONObject() {
    private val harita = LinkedHashMap<String, Any?>()

    constructor(metin: String) : this() {
        val d = Cozumleyici(metin).deger()
        if (d !is JSONObject) throw JSONException("JSON nesnesi değil")
        harita.putAll(d.harita)
    }

    constructor(m: Map<String, Any?>) : this() {
        m.forEach { (k, v) -> put(k, v) }
    }

    companion object {
        val NULL: Any = Bos
        fun quote(s: String): String = StringBuilder().also { yaziEkle(it, s) }.toString()
    }

    fun length(): Int = harita.size
    fun has(k: String): Boolean = harita.containsKey(k)
    fun keys(): Iterator<String> = harita.keys.toList().iterator()
    fun keySet(): Set<String> = harita.keys
    fun opt(k: String): Any? = harita[k]
    fun isNull(k: String): Boolean = harita[k].let { it == null || it === NULL }
    fun remove(k: String): Any? = harita.remove(k)

    fun put(k: String, v: Any?): JSONObject {
        if (v == null) harita.remove(k) else harita[k] = sar(v)
        return this
    }

    fun put(k: String, v: Double): JSONObject {
        if (v.isNaN() || v.isInfinite()) throw JSONException("Geçersiz sayı: $v")
        harita[k] = v; return this
    }

    fun get(k: String): Any = harita[k] ?: throw JSONException("JSONObject[\"$k\"] bulunamadı.")

    fun optString(k: String): String = optString(k, "")
    fun optString(k: String, yedek: String): String = metneCevir(harita[k]) ?: yedek
    fun optInt(k: String): Int = optInt(k, 0)
    fun optInt(k: String, yedek: Int): Int = tamaCevir(harita[k])?.toInt() ?: yedek
    fun optLong(k: String): Long = optLong(k, 0L)
    fun optLong(k: String, yedek: Long): Long = tamaCevir(harita[k]) ?: yedek
    fun optDouble(k: String): Double = optDouble(k, Double.NaN)
    fun optDouble(k: String, yedek: Double): Double = sayiyaCevir(harita[k]) ?: yedek
    fun optBoolean(k: String): Boolean = optBoolean(k, false)
    fun optBoolean(k: String, yedek: Boolean): Boolean = mantigaCevir(harita[k]) ?: yedek
    fun optJSONObject(k: String): JSONObject? = harita[k] as? JSONObject
    fun optJSONArray(k: String): JSONArray? = harita[k] as? JSONArray

    fun getString(k: String): String = metneCevir(get(k)) ?: throw JSONException("$k metin değil")
    fun getInt(k: String): Int = tamaCevir(get(k))?.toInt() ?: throw JSONException("$k sayı değil")
    fun getLong(k: String): Long = tamaCevir(get(k)) ?: throw JSONException("$k sayı değil")
    fun getDouble(k: String): Double = sayiyaCevir(get(k)) ?: throw JSONException("$k sayı değil")
    fun getBoolean(k: String): Boolean = mantigaCevir(get(k)) ?: throw JSONException("$k mantıksal değil")
    fun getJSONObject(k: String): JSONObject = get(k) as? JSONObject ?: throw JSONException("$k nesne değil")
    fun getJSONArray(k: String): JSONArray = get(k) as? JSONArray ?: throw JSONException("$k dizi değil")

    internal fun yaz(sb: StringBuilder) {
        sb.append('{')
        var ilk = true
        for ((k, v) in harita) {
            if (!ilk) sb.append(',')
            ilk = false
            yaziEkle(sb, k); sb.append(':'); degerYaz(sb, v)
        }
        sb.append('}')
    }

    override fun toString(): String = StringBuilder().also { yaz(it) }.toString()
}

class JSONArray() {
    private val liste = ArrayList<Any?>()

    constructor(metin: String) : this() {
        val d = Cozumleyici(metin).deger()
        if (d !is JSONArray) throw JSONException("JSON dizisi değil")
        liste.addAll(d.liste)
    }

    constructor(c: Collection<*>) : this() {
        c.forEach { put(it) }
    }

    fun length(): Int = liste.size
    fun opt(i: Int): Any? = liste.getOrNull(i)
    fun isNull(i: Int): Boolean = opt(i).let { it == null || it === JSONObject.NULL }
    fun put(v: Any?): JSONArray { liste.add(sar(v) ?: JSONObject.NULL); return this }
    fun put(i: Int, v: Any?): JSONArray {
        while (liste.size <= i) liste.add(JSONObject.NULL)
        liste[i] = sar(v) ?: JSONObject.NULL; return this
    }
    fun remove(i: Int): Any? = if (i in liste.indices) liste.removeAt(i) else null
    fun get(i: Int): Any = liste.getOrNull(i) ?: throw JSONException("Dizi[$i] yok")

    fun optString(i: Int): String = optString(i, "")
    fun optString(i: Int, yedek: String): String = metneCevir(opt(i)) ?: yedek
    fun optInt(i: Int): Int = optInt(i, 0)
    fun optInt(i: Int, yedek: Int): Int = tamaCevir(opt(i))?.toInt() ?: yedek
    fun optLong(i: Int): Long = tamaCevir(opt(i)) ?: 0L
    fun optDouble(i: Int): Double = optDouble(i, Double.NaN)
    fun optDouble(i: Int, yedek: Double): Double = sayiyaCevir(opt(i)) ?: yedek
    fun optBoolean(i: Int): Boolean = mantigaCevir(opt(i)) ?: false
    fun optJSONObject(i: Int): JSONObject? = opt(i) as? JSONObject
    fun optJSONArray(i: Int): JSONArray? = opt(i) as? JSONArray
    fun getJSONObject(i: Int): JSONObject = get(i) as? JSONObject ?: throw JSONException("Dizi[$i] nesne değil")
    fun getString(i: Int): String = metneCevir(get(i)) ?: ""
    fun getInt(i: Int): Int = tamaCevir(get(i))?.toInt() ?: throw JSONException("Dizi[$i] sayı değil")

    internal fun yaz(sb: StringBuilder) {
        sb.append('[')
        liste.forEachIndexed { i, v -> if (i > 0) sb.append(','); degerYaz(sb, v) }
        sb.append(']')
    }

    override fun toString(): String = StringBuilder().also { yaz(it) }.toString()
}

/* ------------------------------ yazma ------------------------------ */

private fun yaziEkle(sb: StringBuilder, s: String) {
    sb.append('"')
    for (c in s) {
        when (c) {
            '"' -> sb.append("\\\"")
            '\\' -> sb.append("\\\\")
            '\n' -> sb.append("\\n")
            '\r' -> sb.append("\\r")
            '\t' -> sb.append("\\t")
            '\b' -> sb.append("\\b")
            '\u000C' -> sb.append("\\f")
            else -> if (c < ' ') {
                sb.append("\\u"); sb.append(c.code.toString(16).padStart(4, '0'))
            } else sb.append(c)
        }
    }
    sb.append('"')
}

private fun degerYaz(sb: StringBuilder, v: Any?) {
    when (v) {
        null, JSONObject.NULL -> sb.append("null")
        is JSONObject -> v.yaz(sb)
        is JSONArray -> v.yaz(sb)
        is String -> yaziEkle(sb, v)
        is Boolean, is Int, is Long -> sb.append(v.toString())
        is Double -> when {
            v.isNaN() || v.isInfinite() -> sb.append("null")
            v == kotlin.math.floor(v) && kotlin.math.abs(v) < 1e15 -> sb.append(v.toLong().toString())
            else -> sb.append(v.toString())
        }
        is Number -> sb.append(v.toString())
        else -> yaziEkle(sb, v.toString())
    }
}

/* ------------------------------ okuma ------------------------------ */

private class Cozumleyici(private val s: String) {
    private var i = 0

    fun deger(): Any? {
        bosluk()
        if (i >= s.length) throw JSONException("Beklenmeyen son")
        return when (val c = s[i]) {
            '{' -> nesne()
            '[' -> dizi()
            '"' -> metin()
            't' -> kelime("true", true)
            'f' -> kelime("false", false)
            'n' -> kelime("null", JSONObject.NULL)
            else -> if (c == '-' || c.isDigit()) sayi() else throw JSONException("Beklenmeyen karakter '$c' ($i)")
        }
    }

    private fun bosluk() { while (i < s.length && s[i].isWhitespace()) i++ }

    private fun kelime(k: String, d: Any): Any {
        if (!s.startsWith(k, i)) throw JSONException("Geçersiz değer ($i)")
        i += k.length; return d
    }

    private fun nesne(): JSONObject {
        val o = JSONObject(); i++
        bosluk()
        if (i < s.length && s[i] == '}') { i++; return o }
        while (true) {
            bosluk()
            if (i >= s.length || s[i] != '"') throw JSONException("Anahtar bekleniyordu ($i)")
            val k = metin()
            bosluk()
            if (i >= s.length || s[i] != ':') throw JSONException("':' bekleniyordu ($i)")
            i++
            val d = deger()
            if (d === JSONObject.NULL) o.put(k, JSONObject.NULL) else o.put(k, d)
            bosluk()
            if (i < s.length && s[i] == ',') { i++; continue }
            if (i < s.length && s[i] == '}') { i++; return o }
            throw JSONException("',' ya da '}' bekleniyordu ($i)")
        }
    }

    private fun dizi(): JSONArray {
        val a = JSONArray(); i++
        bosluk()
        if (i < s.length && s[i] == ']') { i++; return a }
        while (true) {
            a.put(deger())
            bosluk()
            if (i < s.length && s[i] == ',') { i++; continue }
            if (i < s.length && s[i] == ']') { i++; return a }
            throw JSONException("',' ya da ']' bekleniyordu ($i)")
        }
    }

    private fun metin(): String {
        i++
        val sb = StringBuilder()
        while (i < s.length) {
            val c = s[i++]
            when (c) {
                '"' -> return sb.toString()
                '\\' -> {
                    if (i >= s.length) break
                    when (val e = s[i++]) {
                        '"' -> sb.append('"'); '\\' -> sb.append('\\'); '/' -> sb.append('/')
                        'b' -> sb.append('\b'); 'f' -> sb.append('\u000C'); 'n' -> sb.append('\n')
                        'r' -> sb.append('\r'); 't' -> sb.append('\t')
                        'u' -> { sb.append(s.substring(i, i + 4).toInt(16).toChar()); i += 4 }
                        else -> sb.append(e)
                    }
                }
                else -> sb.append(c)
            }
        }
        throw JSONException("Kapanmamış metin")
    }

    private fun sayi(): Any {
        val bas = i
        if (s[i] == '-') i++
        while (i < s.length && (s[i].isDigit() || s[i] in ".eE+-")) i++
        val t = s.substring(bas, i)
        if (t.none { it == '.' || it == 'e' || it == 'E' }) {
            t.toIntOrNull()?.let { return it }
            t.toLongOrNull()?.let { return it }
        }
        return t.toDoubleOrNull() ?: throw JSONException("Geçersiz sayı $t")
    }
}

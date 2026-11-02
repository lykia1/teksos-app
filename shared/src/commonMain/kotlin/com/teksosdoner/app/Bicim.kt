package com.teksosdoner.app

import com.teksosdoner.app.json.JSONArray
import com.teksosdoner.app.json.JSONObject

fun tl(d: Double): String = ondalikYaz(if (d.isNaN()) 0.0 else d, 2, TR, binlikli = true) + " ₺"

fun kisaTl(d: Double): String = when {
    d >= 100_000 -> String.format(TR, "%.0fb", d / 1000)
    d >= 1000 -> String.format(TR, "%.1fb", d / 1000)
    d <= 0 -> ""
    else -> d.toInt().toString()
}

fun yuzde(simdi: Double, once: Double): Double? = if (once > 0) (simdi - once) / once * 100 else null

fun JSONArray?.nesneler(): List<JSONObject> = if (this == null) emptyList() else (0 until length()).map { getJSONObject(it) }

fun JSONArray?.metinler(): List<String> = if (this == null) emptyList() else (0 until length()).map { optString(it) }

fun tipAdi(t: String): String = when (t) {
    "table" -> "Masa"
    "takeaway" -> "Al götür"
    "delivery" -> "Paket"
    else -> t
}

fun durumAdi(d: String): String = when (d) {
    "open" -> "Açık"
    "paid" -> "Ödendi"
    "cancelled" -> "İptal"
    else -> d
}

/** "2026-11-01" → "01.11" */
fun kisaTarih(t: String): String = if (t.length >= 10) t.substring(8, 10) + "." + t.substring(5, 7) else t

/** "2026-11-01" → "Paz" */
fun gunAdi(t: String): String = gunKisaAdi(t) ?: kisaTarih(t)

/** "2026-11-01 14:05:00" → "01.11 14:05" */
fun tarihSaat(t: String?): String = if (t == null || t.length < 16 || t == "null") "—" else kisaTarih(t) + " " + t.substring(11, 16)

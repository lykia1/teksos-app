package com.teksosdoner.app

import com.teksosdoner.app.json.JSONArray
import com.teksosdoner.app.json.JSONObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Android'deki org.json / String.format davranışıyla aynı sonuç verdiklerini doğrular. */
class UyumTest {
    @Test
    fun json() {
        val j = JSONObject("""{"ok":true,"n":5,"f":110.0,"s":"12","bos":null,"a":[1,{"x":"y"}],"t":"Ç\"ş\n","u":"ç"}""")
        assertTrue(j.optBoolean("ok"))
        assertEquals(5, j.optInt("n"))
        assertEquals(110.0, j.optDouble("f"))
        assertEquals(110, j.optInt("f"))
        assertEquals(12, j.optInt("s"))
        assertEquals(12.0, j.optDouble("s"))
        assertTrue(j.isNull("bos"))
        assertEquals("null", j.optString("bos"))
        assertTrue(j.isNull("yok"))
        assertEquals("", j.optString("yok"))
        assertEquals("v", j.optString("yok", "v"))
        assertTrue(j.optDouble("yok").isNaN())
        assertEquals(0, j.optInt("bos"))
        assertEquals("y", j.optJSONArray("a")!!.optJSONObject(1)!!.optString("x"))
        assertEquals(1, j.optJSONArray("a")!!.optInt(0))
        assertNull(j.optJSONObject("n"))
        assertEquals("Ç\"ş\n", j.optString("t"))
        assertEquals("ç", j.optString("u"))
        assertEquals("5", j.optString("n"))
        val g = JSONObject().put("a", 1).put("b", 2.5).put("c", 3.0).put("d", "x\"y").put("e", JSONArray(listOf("p", 2))).put("f", true)
        assertEquals("""{"a":1,"b":2.5,"c":3,"d":"x\"y","e":["p",2],"f":true}""", g.toString())
        val k = JSONObject(g.toString())
        assertEquals(g.toString(), k.toString())
        k.remove("a"); assertFalse(k.has("a"))
        assertEquals(2, JSONArray("[1,2]").length())
        assertEquals(-3, JSONObject("""{"x":-3}""").optInt("x"))
        assertEquals(1.5e3, JSONObject("""{"x":1.5e3}""").optDouble("x"))
    }

    @Test
    fun bicim() {
        assertEquals("12,50", String.format(TR, "%.2f", 12.5))
        assertEquals("1.234,57 ₺", tl(1234.567))
        assertEquals("0,00 ₺", tl(0.0))
        assertEquals("-5,10 ₺", tl(-5.1))
        assertEquals("1.000.000,00 ₺", tl(1_000_000.0))
        assertEquals("3", String.format(TR, "%.0f", 2.5))
        assertEquals("2026-01-05", String.format(US, "%04d-%02d-%02d", 2026, 1, 5))
        assertEquals("09:05", String.format(US, "%02d:%02d", 9, 5))
        assertEquals("3,5 saat (x)", String.format(TR, "%.1f saat (%s)", 3.46, "x"))
        assertEquals("12.5", String.format(US, "%.1f", 12.46))
        assertEquals("1,2b", kisaTl(1234.0))
        assertEquals("İSTANBUL", "istanbul".uppercase(TR))
        assertEquals("ılık", "ILIK".lowercase(TR))
    }

    @Test
    fun tarih() {
        assertEquals("Paz", gunAdi("2026-11-01"))
        assertEquals("Pzt", gunAdi("2026-11-02"))
        assertEquals("Per", gunAdi("2024-02-29"))
        assertEquals("2026-03-01", tarihEkle("2026-02-28", 1))
        assertEquals("2024-02-29", tarihEkle("2024-03-01", -1))
        assertEquals("2027-01-01", tarihEkle("2026-12-31", 1))
        assertEquals("2026-01", ayEkle("2025-12", 1))
        assertEquals("2025-12", ayEkle("2026-01", -1))
        assertEquals("Kasım", ayAdi(11))
        assertEquals("%C3%A7+%26", urlKodla("ç &").replace("%20", "+"))
        assertEquals(32, rastgeleKimlik().length)
    }
}

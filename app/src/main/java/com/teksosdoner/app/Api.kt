package com.teksosdoner.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class ApiHata(val kod: Int, mesaj: String) : Exception(mesaj)

/** Sunucudaki app_api.php ile konuşur. */
object Api {
    const val UA = "TeksosApp/2.0 TeksosNative Android"
    val sunucu: String = BuildConfig.SUNUCU
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(40, TimeUnit.SECONDS)
        .build()

    suspend fun get(rota: String, parametre: Map<String, String> = emptyMap()): JSONObject = istek(rota, parametre, null)

    suspend fun post(rota: String, govde: JSONObject = JSONObject()): JSONObject = istek(rota, emptyMap(), govde)

    private suspend fun istek(rota: String, parametre: Map<String, String>, govde: JSONObject?): JSONObject =
        withContext(Dispatchers.IO) {
            val url = (sunucu + "app_api.php").toHttpUrl().newBuilder().addQueryParameter("r", rota).apply {
                parametre.forEach { (k, v) -> if (v.isNotEmpty()) addQueryParameter(k, v) }
            }.build()
            val b = Request.Builder().url(url).header("User-Agent", UA).header("Accept", "application/json")
            Oturum.anahtar?.let { b.header("Authorization", "Bearer $it") }
            Oturum.sube?.let { b.header("X-Sube", it) }
            if (govde != null) b.post(govde.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            val (kod, metin) = try {
                http.newCall(b.build()).execute().use { y -> y.code to (y.body?.string() ?: "") }
            } catch (e: IOException) {
                throw ApiHata(0, "İnternet bağlantısı yok. Bağlantıyı kontrol edip tekrar deneyin.")
            }
            val j = try {
                JSONObject(metin)
            } catch (e: Exception) {
                throw ApiHata(kod, "Sunucudan beklenmeyen cevap ($kod). Site güncel mi?")
            }
            if (!j.optBoolean("ok", false)) {
                if (kod == 401 && Oturum.anahtar != null && rota != "giris" && rota != "giris_kod") {
                    withContext(Dispatchers.Main) { Oturum.cikis() }
                }
                throw ApiHata(kod, j.optString("hata", "Bir hata oluştu."))
            }
            j
        }
}

package com.teksosdoner.app

import com.teksosdoner.app.json.JSONObject
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/** Android'de OkHttp, iPhone'da Apple'ın kendi ağ katmanı */
expect fun httpMotoru(): io.ktor.client.engine.HttpClientEngineFactory<*>

class ApiHata(val kod: Int, mesaj: String) : Exception(mesaj)

/** Sunucudaki app_api.php ile konuşur. */
object Api {
    val UA: String get() = "TeksosApp/3.0 TeksosNative " + os.ad
    val sunucu: String = Ayar.SUNUCU
    val http: HttpClient by lazy {
        HttpClient(httpMotoru()) {
            expectSuccess = false
            install(HttpTimeout) {
                connectTimeoutMillis = 15_000
                requestTimeoutMillis = 45_000
                socketTimeoutMillis = 40_000
            }
        }
    }

    suspend fun get(rota: String, parametre: Map<String, String> = emptyMap()): JSONObject = istek(rota, parametre, null)

    suspend fun post(rota: String, govde: JSONObject = JSONObject()): JSONObject = istek(rota, emptyMap(), govde)

    /** Yerli kasa ekranı: kasa/api.php işlemleri (eylem = bootstrap, save, checkout …). */
    suspend fun kasa(eylem: String, govde: JSONObject? = null, parametre: Map<String, String> = emptyMap()): JSONObject =
        istek("kasa", parametre + ("eylem" to eylem), govde)

    private suspend fun istek(rota: String, parametre: Map<String, String>, govde: JSONObject?): JSONObject =
        withContext(Dispatchers.IO) {
            val url = sunucu + "app_api.php"
            val (kod, metin) = try {
                val y = if (govde == null) http.get(url) { ayarla(rota, parametre) }
                else http.post(url) {
                    ayarla(rota, parametre)
                    contentType(ContentType.Application.Json)
                    setBody(govde.toString())
                }
                y.status.value to y.bodyAsText()
            } catch (e: Throwable) {
                if (e is kotlinx.coroutines.CancellationException) throw e
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
                throw ApiHata(kod, j.optString("hata", j.optString("error", "Bir hata oluştu.")))
            }
            j
        }

    private fun io.ktor.client.request.HttpRequestBuilder.ayarla(rota: String, parametre: Map<String, String>) {
        parameter("r", rota)
        parametre.forEach { (k, v) -> if (v.isNotEmpty()) parameter(k, v) }
        header("User-Agent", UA)
        header("Accept", "application/json")
        Oturum.anahtar?.let { header("Authorization", "Bearer $it") }
        Oturum.sube?.let { header("X-Sube", it) }
    }
}

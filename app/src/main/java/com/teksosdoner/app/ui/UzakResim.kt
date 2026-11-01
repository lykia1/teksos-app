package com.teksosdoner.app.ui

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.teksosdoner.app.Api
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/** Küçük, bağımlılıksız resim yükleyici (TV panosu önizlemesi için). */
object ResimYukleyici {
    private val http = OkHttpClient()
    private val bellek = object : LruCache<String, ImageBitmap>(24) {}

    suspend fun al(url: String): ImageBitmap? {
        bellek.get(url)?.let { return it }
        return withContext(Dispatchers.IO) {
            runCatching {
                http.newCall(Request.Builder().url(url).header("User-Agent", Api.UA).build()).execute().use { y ->
                    val bayt = y.body?.bytes() ?: return@use null
                    val sinir = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(bayt, 0, bayt.size, sinir)
                    var oran = 1
                    while (sinir.outWidth / oran > 1600) oran *= 2
                    BitmapFactory.decodeByteArray(bayt, 0, bayt.size, BitmapFactory.Options().apply { inSampleSize = oran })?.asImageBitmap()
                }
            }.getOrNull()?.also { bellek.put(url, it) }
        }
    }
}

@Composable
fun UzakResim(url: String?, modifier: Modifier = Modifier, olcek: ContentScale = ContentScale.Fit) {
    val resim by produceState<ImageBitmap?>(null, url) { value = url?.let { ResimYukleyici.al(it) } }
    resim?.let { Image(it, null, modifier, contentScale = olcek) }
}

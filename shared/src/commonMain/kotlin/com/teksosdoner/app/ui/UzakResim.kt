package com.teksosdoner.app.ui

import com.teksosdoner.app.*
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.readRawBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/** Küçük, bağımlılıksız resim yükleyici (TV panosu önizlemesi için). */
object ResimYukleyici {
    private val bellek = LinkedHashMap<String, ImageBitmap>()

    suspend fun al(url: String): ImageBitmap? {
        bellek[url]?.let { return it }
        return withContext(Dispatchers.IO) {
            runCatching {
                val bayt = Api.http.get(url) { header("User-Agent", Api.UA) }.readRawBytes()
                os.resimCoz(bayt)
            }.getOrNull()
        }?.also {
            bellek[url] = it
            if (bellek.size > 24) bellek.remove(bellek.keys.first())
        }
    }
}

@Composable
fun UzakResim(url: String?, modifier: Modifier = Modifier, olcek: ContentScale = ContentScale.Fit) {
    val resim by produceState<ImageBitmap?>(null, url) { value = url?.let { ResimYukleyici.al(it) } }
    resim?.let { Image(it, null, modifier, contentScale = olcek) }
}

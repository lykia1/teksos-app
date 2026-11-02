package com.teksosdoner.app

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

actual fun yerelZaman(): String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())

actual fun simdiMs(): Long = System.currentTimeMillis()

actual fun httpMotoru(): io.ktor.client.engine.HttpClientEngineFactory<*> = io.ktor.client.engine.android.Android

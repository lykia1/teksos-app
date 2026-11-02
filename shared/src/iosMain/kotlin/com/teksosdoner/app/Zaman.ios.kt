package com.teksosdoner.app

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.timeIntervalSince1970

actual fun yerelZaman(): String {
    val f = NSDateFormatter()
    f.locale = NSLocale(localeIdentifier = "en_US_POSIX")
    f.dateFormat = "yyyy-MM-dd HH:mm:ss"
    return f.stringFromDate(NSDate())
}

actual fun simdiMs(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()

actual fun httpMotoru(): io.ktor.client.engine.HttpClientEngineFactory<*> = io.ktor.client.engine.darwin.Darwin

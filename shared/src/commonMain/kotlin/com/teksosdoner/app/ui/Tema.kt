package com.teksosdoner.app.ui

import com.teksosdoner.app.*

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Turuncu = Color(0xFFE8590C)
val TuruncuAcik = Color(0xFFFFF4EC)
val Koyu = Color(0xFF14110F)
val Koyu2 = Color(0xFF292524)
val Zemin = Color(0xFFF6F3EE)
val Cizgi = Color(0xFFE7E1D8)
val Soluk = Color(0xFF78716C)
val Murekkep = Color(0xFF1C1917)
val Yesil = Color(0xFF15803D)
val YesilAcik = Color(0xFFDCFCE7)
val Kirmizi = Color(0xFFDC2626)
val KirmiziAcik = Color(0xFFFEE2E2)
val Sari = Color(0xFFB45309)
val SariAcik = Color(0xFFFEF3C7)
val Mavi = Color(0xFF1D4ED8)
val MaviAcik = Color(0xFFDBEAFE)

@Composable
fun TeksosTema(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Turuncu,
            onPrimary = Color.White,
            primaryContainer = TuruncuAcik,
            onPrimaryContainer = Color(0xFF9A3412),
            secondary = Koyu,
            onSecondary = Color.White,
            secondaryContainer = TuruncuAcik,
            onSecondaryContainer = Color(0xFF9A3412),
            background = Zemin,
            onBackground = Murekkep,
            surface = Color.White,
            onSurface = Murekkep,
            surfaceVariant = Color(0xFFF5F5F4),
            onSurfaceVariant = Soluk,
            surfaceContainer = Color.White,
            surfaceContainerHigh = Color.White,
            surfaceContainerLow = Color.White,
            outline = Cizgi,
            outlineVariant = Cizgi,
            error = Kirmizi,
        ),
        content = content
    )
}

package com.teksosdoner.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.teksosdoner.app.Oturum

/** Uygulamanın kökü (Android MainActivity ve iPhone MainViewController aynı ekranı açar). */
@Composable
fun Uygulama() {
    TeksosTema {
        val girisli by Oturum.girisli.collectAsState()
        if (girisli) AnaEkran() else GirisEkrani()
        SeciciKatmani()
    }
}

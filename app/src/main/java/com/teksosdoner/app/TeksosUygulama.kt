package com.teksosdoner.app

import android.app.Application
import com.onesignal.OneSignal
import com.onesignal.notifications.INotificationClickEvent
import com.onesignal.notifications.INotificationClickListener
import kotlinx.coroutines.flow.MutableStateFlow

/** Bildirime dokunulunca açılacak sayfa (ör. kurye.php → Paketler ekranı). */
object Bildirim {
    val bekleyen = MutableStateFlow<String?>(null)
}

class TeksosUygulama : Application() {
    override fun onCreate() {
        super.onCreate()
        Oturum.baslat(this)
        OneSignal.initWithContext(this, BuildConfig.ONESIGNAL_ID)
        OneSignal.Notifications.addClickListener(object : INotificationClickListener {
            override fun onClick(event: INotificationClickEvent) {
                val url = event.notification.additionalData?.optString("url").orEmpty()
                if (url.isNotEmpty()) Bildirim.bekleyen.value = url
            }
        })
    }
}

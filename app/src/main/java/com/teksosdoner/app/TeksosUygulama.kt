package com.teksosdoner.app

import android.app.Application
import com.onesignal.OneSignal
import com.onesignal.notifications.INotificationClickEvent
import com.onesignal.notifications.INotificationClickListener

class TeksosUygulama : Application() {
    override fun onCreate() {
        super.onCreate()
        os = AndroidPlatform(this)
        Oturum.baslat()
        OneSignal.initWithContext(this, Ayar.ONESIGNAL_ID)
        OneSignal.Notifications.addClickListener(object : INotificationClickListener {
            override fun onClick(event: INotificationClickEvent) {
                val url = event.notification.additionalData?.optString("url").orEmpty()
                if (url.isNotEmpty()) Bildirim.bekleyen.value = url
            }
        })
    }
}

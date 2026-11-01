package com.teksosdoner.app;

import android.app.Application;

import androidx.annotation.NonNull;

import com.onesignal.OneSignal;
import com.onesignal.notifications.INotificationClickEvent;
import com.onesignal.notifications.INotificationClickListener;

import org.json.JSONObject;

/** Uygulama açılışı: OneSignal bildirimleri ve bildirime dokununca ilgili sayfanın açılması. */
public class Uygulama extends Application {
    public static final String ONESIGNAL_APP_ID = "7fb5f933-2168-419a-8fe8-c4b9cfad13b4";
    public static volatile String bekleyenUrl = null;

    @Override
    public void onCreate() {
        super.onCreate();
        OneSignal.initWithContext(this, ONESIGNAL_APP_ID);
        OneSignal.getNotifications().addClickListener(new INotificationClickListener() {
            @Override
            public void onClick(@NonNull INotificationClickEvent event) {
                JSONObject veri = event.getNotification().getAdditionalData();
                String url = veri != null ? veri.optString("url", "") : "";
                if (!url.isEmpty()) {
                    bekleyenUrl = url;
                    MainActivity.urlAc();
                }
            }
        });
    }
}

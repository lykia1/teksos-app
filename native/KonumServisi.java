package com.teksosdoner.app;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Kurye konumu — telefon kilitliyken de çalışan ön plan servisi.
 * Bildirim çubuğunda "Konumunuz paylaşılıyor" yazar; kurye ekranındaki düğmeyle kapanır.
 * Konum her ~20 sn'de sunucudaki kurye_konum.php'ye gönderilir.
 */
public class KonumServisi extends Service {
    static volatile boolean calisiyor = false;
    private static final String KANAL = "konum";
    private static final int NO = 4711;
    private static final String AYAR = "teksos_konum";

    private FusedLocationProviderClient istemci;
    private LocationCallback geriCagri;
    private final ExecutorService is = Executors.newSingleThreadExecutor();
    private volatile String url, kod;

    static void ayarla(Context c, String url, String kod, int aralik, boolean aktif) {
        SharedPreferences.Editor e = c.getSharedPreferences(AYAR, MODE_PRIVATE).edit();
        if (url != null) e.putString("url", url);
        if (kod != null) e.putString("kod", kod);
        if (aralik > 0) e.putInt("aralik", aralik);
        e.putBoolean("aktif", aktif).apply();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!onPlanaGec()) {
            return START_NOT_STICKY;
        }
        SharedPreferences sp = getSharedPreferences(AYAR, MODE_PRIVATE);
        url = sp.getString("url", null);
        kod = sp.getString("kod", null);
        int aralik = Math.max(10, sp.getInt("aralik", 20));
        if (!sp.getBoolean("aktif", false) || url == null || kod == null) {
            durdur();
            return START_NOT_STICKY;
        }
        if (geriCagri == null) {
            baslat(aralik);
        }
        calisiyor = true;
        return START_STICKY;
    }

    private boolean onPlanaGec() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (Build.VERSION.SDK_INT >= 26 && nm != null && nm.getNotificationChannel(KANAL) == null) {
            NotificationChannel ch = new NotificationChannel(KANAL, "Kurye konumu", NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("Kurye ekranı açıkken konum paylaşımı");
            nm.createNotificationChannel(ch);
        }
        Intent ac = new Intent(this, MainActivity.class);
        ac.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(this, 0, ac, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification n = new NotificationCompat.Builder(this, KANAL)
            .setSmallIcon(R.drawable.ic_bildirim)
            .setContentTitle("Teksos · Kurye")
            .setContentText("Konumunuz paylaşılıyor")
            .setOngoing(true)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build();
        try {
            int tip = Build.VERSION.SDK_INT >= 29 ? ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION : 0;
            ServiceCompat.startForeground(this, NO, n, tip);
            return true;
        } catch (Exception e) {
            calisiyor = false;
            stopSelf();
            return false;
        }
    }

    @SuppressLint("MissingPermission")
    private void baslat(int aralik) {
        istemci = LocationServices.getFusedLocationProviderClient(this);
        LocationRequest lr = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, aralik * 1000L)
            .setMinUpdateIntervalMillis(aralik * 500L)
            .build();
        geriCagri = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult sonuc) {
                Location l = sonuc.getLastLocation();
                if (l != null) gonder(l);
            }
        };
        try {
            istemci.requestLocationUpdates(lr, geriCagri, Looper.getMainLooper());
        } catch (SecurityException e) {
            durdur();
        }
    }

    private void gonder(Location l) {
        final String hedef = url, k = kod;
        if (hedef == null || k == null) return;
        final String govde = String.format(Locale.US,
            "{\"device_id\":\"%s\",\"location\":{\"coords\":{\"latitude\":%.7f,\"longitude\":%.7f,\"accuracy\":%.1f,\"speed\":%.2f}}}",
            k, l.getLatitude(), l.getLongitude(), l.getAccuracy(), l.hasSpeed() ? l.getSpeed() : -1f);
        is.execute(() -> {
            HttpURLConnection c = null;
            try {
                c = (HttpURLConnection) new URL(hedef).openConnection();
                c.setConnectTimeout(15000);
                c.setReadTimeout(15000);
                c.setRequestMethod("POST");
                c.setDoOutput(true);
                c.setRequestProperty("Content-Type", "application/json");
                c.setRequestProperty("User-Agent", "TeksosApp/1.0");
                try (OutputStream o = c.getOutputStream()) {
                    o.write(govde.getBytes(StandardCharsets.UTF_8));
                }
                if (c.getResponseCode() == 403) {
                    // Kod artık geçerli değil (kurye silindi / pasif) → paylaşımı bırak
                    ayarla(KonumServisi.this, null, null, 0, false);
                    new Handler(Looper.getMainLooper()).post(this::durdur);
                }
            } catch (Exception e) {
                // internet yoksa bu konum atlanır, sonraki denenir
            } finally {
                if (c != null) c.disconnect();
            }
        });
    }

    private void durdur() {
        if (istemci != null && geriCagri != null) istemci.removeLocationUpdates(geriCagri);
        geriCagri = null;
        calisiyor = false;
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        if (istemci != null && geriCagri != null) istemci.removeLocationUpdates(geriCagri);
        geriCagri = null;
        calisiyor = false;
        is.shutdown();
        super.onDestroy();
    }
}

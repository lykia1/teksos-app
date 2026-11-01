package com.teksosdoner.app;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.PowerManager;
import android.provider.Settings;

import androidx.core.content.ContextCompat;

import com.getcapacitor.JSObject;
import com.getcapacitor.PermissionState;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;
import com.onesignal.Continue;
import com.onesignal.OneSignal;

/** Sitedeki sayfaların telefona eriştiği köprü: window.Capacitor.Plugins.Teksos */
@CapacitorPlugin(
    name = "Teksos",
    permissions = {
        @Permission(alias = "konum", strings = { Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION })
    }
)
public class TeksosPlugin extends Plugin {

    @PluginMethod
    public void bilgi(PluginCall call) {
        JSObject r = new JSObject();
        String surum = "";
        try {
            surum = getContext().getPackageManager().getPackageInfo(getContext().getPackageName(), 0).versionName;
        } catch (Exception e) {
            // sürüm okunamadı
        }
        r.put("surum", surum);
        r.put("konum", KonumServisi.calisiyor);
        PowerManager pm = (PowerManager) getContext().getSystemService(Context.POWER_SERVICE);
        r.put("pilSerbest", pm != null && pm.isIgnoringBatteryOptimizations(getContext().getPackageName()));
        call.resolve(r);
    }

    /** Giriş yapan kişinin bildirim kimliği (sunucudaki push_kimlik ile aynı). */
    @PluginMethod
    public void pushGiris(PluginCall call) {
        String id = call.getString("id", "");
        if (id != null && !id.isEmpty()) {
            OneSignal.login(id);
            OneSignal.getNotifications().requestPermission(false, Continue.none());
        }
        call.resolve();
    }

    @PluginMethod
    public void pushCikis(PluginCall call) {
        OneSignal.logout();
        call.resolve();
    }

    @PluginMethod
    public void konumBaslat(PluginCall call) {
        if (getPermissionState("konum") != PermissionState.GRANTED) {
            requestPermissionForAlias("konum", call, "konumIzni");
            return;
        }
        baslat(call);
    }

    @PermissionCallback
    private void konumIzni(PluginCall call) {
        if (getPermissionState("konum") == PermissionState.GRANTED) {
            baslat(call);
        } else {
            call.reject("Konum izni verilmedi");
        }
    }

    private void baslat(PluginCall call) {
        String url = call.getString("url", "");
        String kod = call.getString("kod", "");
        Integer aralik = call.getInt("aralik", 20);
        if (url == null || url.isEmpty() || kod == null || kod.isEmpty()) {
            call.reject("Adres veya kod eksik");
            return;
        }
        KonumServisi.ayarla(getContext(), url, kod, aralik == null ? 20 : aralik, true);
        try {
            ContextCompat.startForegroundService(getContext(), new Intent(getContext(), KonumServisi.class));
            call.resolve();
        } catch (Exception e) {
            call.reject("Konum servisi başlatılamadı: " + e.getMessage());
        }
    }

    @PluginMethod
    public void konumDurdur(PluginCall call) {
        KonumServisi.ayarla(getContext(), null, null, 0, false);
        getContext().stopService(new Intent(getContext(), KonumServisi.class));
        call.resolve();
    }

    /** Pil tasarrufu uygulamayı durdurmasın diye izin ekranı (bir kez sorulur). */
    @PluginMethod
    public void pilAyari(PluginCall call) {
        Context c = getContext();
        PowerManager pm = (PowerManager) c.getSystemService(Context.POWER_SERVICE);
        if (pm != null && !pm.isIgnoringBatteryOptimizations(c.getPackageName())) {
            try {
                Intent i = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                i.setData(Uri.parse("package:" + c.getPackageName()));
                getActivity().startActivity(i);
            } catch (Exception e) {
                // bazı telefonlarda bu ekran yok
            }
        }
        call.resolve();
    }
}

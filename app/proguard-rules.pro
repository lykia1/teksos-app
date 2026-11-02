# Teksos uygulama kodu (servis, eklenti ve Compose ekranları) korunur
-keep class com.teksosdoner.app.** { *; }
# OneSignal kendi kurallarını getirir; isteğe bağlı bağımlılıkları için uyarılar susturulur
-dontwarn com.onesignal.**
-dontwarn com.huawei.**
-dontwarn com.amazon.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# Bildirim kütüphanesi küçültülmeden olduğu gibi kalsın (cihazda test edilemediği için güvenli tercih)
-keep class com.onesignal.** { *; }
-keep interface com.onesignal.** { *; }

# Ağ kütüphanesi (Ktor) isteğe bağlı günlük sınıfları
-dontwarn org.slf4j.**
-dontwarn java.lang.management.**

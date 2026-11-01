plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.teksosdoner.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.teksosdoner.app"
        minSdk = 26
        targetSdk = 35
        // Önceki (web tabanlı) sürümün üstüne kurulabilsin diye sürüm kodu her derlemede artar
        versionCode = (System.getenv("VCODE") ?: "100").toInt()
        versionName = System.getenv("VNAME") ?: "2.0.0"
        buildConfigField("String", "SUNUCU", "\"https://teksosdonerbesevler.com/\"")
        buildConfigField("String", "ONESIGNAL_ID", "\"7fb5f933-2168-419a-8fe8-c4b9cfad13b4\"")
    }

    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("KS_FILE") ?: "teksos.jks")
            storePassword = System.getenv("KS_PASS")
            keyAlias = "teksos"
            keyPassword = System.getenv("KS_PASS")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.onesignal:OneSignal:[5.1.6, 5.99.99]")
}

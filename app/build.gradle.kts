plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.teksosdoner.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.teksosdoner.app"
        minSdk = 26
        targetSdk = 35
        // Önceki (web tabanlı) sürümün üstüne kurulabilsin diye sürüm kodu her derlemede artar
        versionCode = (System.getenv("VCODE") ?: "100").toInt()
        versionName = System.getenv("VNAME") ?: "2.0.0"
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
            // Kullanılmayan kod ve ikonlar atılır (uygulama küçük ve hızlı olsun)
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }

    // Lint, Kotlin 2.4 ile derlenen ortak modülü okuyamıyor; yayın derlemesini durdurmasın
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":shared"))
    implementation("androidx.activity:activity-compose:1.12.4")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.onesignal:OneSignal:[5.1.6, 5.99.99]")
}

kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}

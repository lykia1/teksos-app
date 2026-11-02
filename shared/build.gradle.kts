import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/*
 * Ortak modül: bütün ekranlar ve sunucu bağlantısı burada.
 * Android uygulaması (app) ve iPhone uygulaması (iosApp) aynı kodu kullanır.
 */
plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.library")
    id("org.jetbrains.kotlin.plugin.compose")
}

val compose = "1.9.3"
val ktor = "3.3.3"

kotlin {
    androidTarget {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }
    listOf(iosArm64(), iosSimulatorArm64()).forEach { t ->
        t.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        optIn.add("androidx.compose.material3.ExperimentalMaterial3Api")
        optIn.add("androidx.compose.foundation.layout.ExperimentalLayoutApi")
    }
    sourceSets {
        commonMain.dependencies {
            api("org.jetbrains.compose.runtime:runtime:$compose")
            api("org.jetbrains.compose.foundation:foundation:$compose")
            api("org.jetbrains.compose.ui:ui:$compose")
            api("org.jetbrains.compose.material3:material3:1.9.0")
            api("org.jetbrains.compose.material:material-icons-extended:1.7.3")
            implementation("org.jetbrains.androidx.navigation:navigation-compose:2.9.1")
            api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
            api("io.ktor:ktor-client-core:$ktor")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        androidMain.dependencies {
            implementation("io.ktor:ktor-client-android:$ktor")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
        }
        iosMain.dependencies {
            implementation("io.ktor:ktor-client-darwin:$ktor")
        }
    }
}

android {
    namespace = "com.teksosdoner.app.shared"
    compileSdk = 36
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

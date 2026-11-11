plugins {
    id("com.android.application")
}

android {
    namespace = "uk.me.jamez.doovdroid"
    compileSdk = 36

    defaultConfig {
        applicationId = "uk.me.jamez.doovdroid"
        // The phone is Android 14 (SDK 34). Targeting 34 keeps its behaviour exactly what we test.
        minSdk = 34
        targetSdk = 34
        versionCode = 2
        versionName = "0.2"
    }

    buildTypes {
        // Debug-signed with ~/.android/debug.keystore: a private build for one phone. Keep the same
        // keystore or the next install can't update in place (it would need an uninstall).
        debug { }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

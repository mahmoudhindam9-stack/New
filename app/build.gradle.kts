plugins {
    id("com.android.application")
}

android {
    namespace = "com.example.blackscreen"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.blackscreen"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}

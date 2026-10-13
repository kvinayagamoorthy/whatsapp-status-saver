plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.moorthy.statussaver"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.moorthy.statussaver"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            // Minification and signing are configured in the release milestone (T6.2).
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

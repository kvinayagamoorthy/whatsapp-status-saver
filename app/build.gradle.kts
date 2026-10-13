plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
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

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)

    // Screen navigation and ViewModel/lifecycle integration for Compose.
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Persisted folder URIs and theme preference.
    implementation(libs.androidx.datastore.preferences)

    // Thumbnails for images and video frames. No Coil network module: the app is offline.
    implementation(libs.coil.compose)
    implementation(libs.coil.video)

    // Video playback in the viewer.
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
}

import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Release signing (D-038): the keystore lives outside the repo, and its path and passwords
// in the git-ignored keystore.properties at the repo root. Without that file the release
// APK is built unsigned.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.isFile) {
        keystorePropertiesFile.inputStream().use { input -> load(input) }
    }
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

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (keystorePropertiesFile.isFile) {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            // R8 removes unused code and resources, which keeps the sideloaded APK small
            // (NFR5). Libraries bring their own keep rules; the app needs none so far.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        // Lets Robolectric read the merged manifest, so tests run on the target SDK
        // instead of Robolectric's oldest default.
        unitTests.isIncludeAndroidResources = true
        unitTests.all { test ->
            // Robolectric's Android 16+ runtimes reach into this JDK-internal package.
            test.jvmArgs("--add-exports=java.base/jdk.internal.access=ALL-UNNAMED")
        }
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

    // Video playback in the viewer, with Compose Material 3 controls (D-025).
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui.compose.material3)

    // JVM unit tests (src/test). Robolectric provides working Android classes such as
    // Uri and ContentResolver persisted permissions on the JVM (DECISIONS D-014).
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    // Replaces the main dispatcher so ViewModel coroutines run under test control.
    testImplementation(libs.kotlinx.coroutines.test)

    // Instrumented tests (src/androidTest) for what only a real device shows, such as
    // where MediaStore puts a saved file.
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "app.cbdm.qrcodedisplayer"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "app.cbdm.qrcodedisplayer"
        minSdk = 24
        targetSdk = 37
        versionCode = 2
        versionName = "1.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // Default dependencies from Android Studio
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    // Quick QR code scanner library for a first try
    implementation("io.github.zxing-cpp:android:3.1.1")
    // "Heavier" QR code scanner library for trickier images
    implementation("org.boofcv:boofcv-android:1.5.0")
    implementation("org.boofcv:boofcv-core:1.5.0")
    // ZXing for drawing the clean, new QR code
    implementation("com.google.zxing:core:3.5.4")
    // For the bug icon
    implementation("androidx.compose.material:material-icons-extended")
}
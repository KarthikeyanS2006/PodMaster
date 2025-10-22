// File: app/build.gradle.kts
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
}

android {
    namespace = "com.podmaster"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.podmaster"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    buildFeatures {
        viewBinding = true
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
    // Core Android - Using version catalog
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    // Additional UI components
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // TensorFlow Lite for AI Noise Suppression
    implementation("org.tensorflow:tensorflow-lite:2.14.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
    implementation("org.tensorflow:tensorflow-lite-gpu:2.14.0")

    // Networking
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // ViewModel and LiveData
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")

    // Waveform visualization
    implementation("com.github.lincollincol:amplituda:2.2.2")

    // Preferences
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("ai.picovoice:koala-android:2.0.2")

    implementation("com.cleveroad:audiovisualization:1.0.0")
    // Or use compose-audiowaveform for Jetpack Compose
    implementation("io.github.lincollincol:compose-audiowaveform:1.1.0")
    
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation("com.google.firebase:firebase-storage-ktx")

    implementation("com.google.apis:google-api-services-youtube:v3-rev20240916-2.0.0")
    implementation("com.google.api-client:google-api-client-android:2.7.0")

    // Add missing dependencies
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    implementation("com.spotify.android:auth:2.1.1")
    implementation("com.spotify.android:spotify-player:2.1.0")
    
    // FFmpeg support
    implementation("com.arthenica:mobile-ffmpeg-full:4.4.LTS")

    // Testing - Using version catalog
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

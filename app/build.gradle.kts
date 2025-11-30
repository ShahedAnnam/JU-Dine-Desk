plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    id("kotlin-parcelize")
}

android {
    namespace = "com.example.judinedesk"
    compileSdk = 34  // Changed from 36 to 34 (stable version)

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.example.judinedesk"
        minSdk = 26
        targetSdk = 34  // Changed from 36 to 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val geminiApiKey = project.findProperty("GEMINI_API_KEY") as String? ?: ""
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")

        // Enable multidex
        multiDexEnabled = true
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    // Multidex - ADD THIS FIRST
    implementation("androidx.multidex:multidex:2.0.1")

    // AndroidX & Material - UPDATED VERSIONS
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.activity:activity-ktx:1.8.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Networking
    implementation("com.squareup.okhttp3:okhttp:4.12.0") // Updated to stable version
    implementation("com.google.code.gson:gson:2.10.1") // Updated version

    // Firebase - SIMPLIFIED (remove individual versions when using BOM)
    implementation(platform("com.google.firebase:firebase-bom:32.7.0")) // Updated BOM version
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")

    // Animation
    implementation("com.airbnb.android:lottie:6.1.0")

    // QR Code
    implementation("com.google.zxing:core:3.4.1") // Stable version

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
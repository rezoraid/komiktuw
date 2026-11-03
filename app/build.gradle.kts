plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.komikta"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.komikta"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "BASE_URL", "\"https://web-production-b2a1d.up.railway.app\"")
        buildConfigField("String", "DEFAULT_SOURCE", "\"lumos\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${(project.findProperty("GOOGLE_WEB_CLIENT_ID") as String?).orEmpty()}\"")
    }

    signingConfigs {
        // Custom debug keystore hanya dipakai jika file ada (lokal).
        // Di CI/GitHub Actions file sering tidak di-commit → fallback ke default Android debug keystore.
        val debugKs = file("komikta-debug.jks")
        if (debugKs.exists()) {
            getByName("debug") {
                storeFile = debugKs
                storePassword = "komikta123"
                keyAlias = "komikta"
                keyPassword = "komikta123"
            }
        }
    }

    val releaseStore = System.getenv("KOMIKTA_STORE_FILE") ?: (project.findProperty("KOMIKTA_STORE_FILE") as String?)
    if (releaseStore != null && file(releaseStore).exists()) {
        signingConfigs.create("release") {
            storeFile = file(releaseStore)
            storePassword = System.getenv("KOMIKTA_STORE_PASSWORD") ?: (project.findProperty("KOMIKTA_STORE_PASSWORD") as String?)
            keyAlias = System.getenv("KOMIKTA_KEY_ALIAS") ?: (project.findProperty("KOMIKTA_KEY_ALIAS") as String?)
            keyPassword = System.getenv("KOMIKTA_KEY_PASSWORD") ?: (project.findProperty("KOMIKTA_KEY_PASSWORD") as String?)
        }
    }

    buildTypes {
        release {
            // Pakai release keystore jika ada; kalau tidak, biarkan unsigned / pakai debug default.
            signingConfigs.findByName("release")?.let { signingConfig = it }
                ?: signingConfigs.findByName("debug")?.let { signingConfig = it }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            // Jangan override signingConfig jika custom keystore tidak ada → Android memakai default debug.keystore
            isDebuggable = true
            isMinifyEnabled = false
            isShrinkResources = false
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.profileinstaller:profileinstaller:1.4.1")

    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")

    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.3")

    implementation("com.google.dagger:hilt-android:2.52")
    ksp("com.google.dagger:hilt-compiler:2.52")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("io.coil-kt:coil:2.7.0")

    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("io.mockk:mockk:1.13.13")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}

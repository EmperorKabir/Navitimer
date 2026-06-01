plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.navitimerguide.wear"
    compileSdk = 35

    // Same applicationId as the phone module (one app identity). No
    // longer embedded in the phone APK, so the versionCode is now
    // independent of the phone's (wear band: 9, …). Sideload: adb install
    // this wear APK directly to the watch. Mirrors the Slide Rule fork's
    // multi-APK form-factor split.
    defaultConfig {
        applicationId = "com.navitimerguide"
        minSdk = 30
        targetSdk = 35
        versionCode = 11
        versionName = "1.0.5"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += listOf("arm64-v8a")
            // Emulator testing on x86_64 hosts: `-PemuAbi` appends x86_64
            // so native libs (graphics.path, datastore) load on the
            // emulator. Distribution builds omit the flag → arm64-only.
            if (project.hasProperty("emuAbi")) abiFilters += "x86_64"
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Sideload-only distribution: no keystore.properties is used.
            // Mirrors the phone module — release falls back to the debug
            // signing config (suitable for direct APK install, not Play).
            signingConfig = signingConfigs.getByName("debug")
            ndk {
                debugSymbolLevel = "NONE"
            }
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
    }

    sourceSets {
        getByName("main").java.srcDirs("src/main/kotlin")
        getByName("test").java.srcDirs("src/test/kotlin")
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Compose BOM (reused from phone catalog) covers core compose UI / foundation.
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)

    // Required by the ported DialViewModel / DialCanvas (used by the
    // chronograph clock and the live time-hands layer).
    implementation(libs.kotlinx.datetime)

    // Bezel sync: phone↔watch Data Layer + persisted sync toggle.
    implementation(libs.play.services.wearable)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.play.services)

    // Wear-specific Compose libraries. Version-pinned here (not in the
    // shared catalog) because the phone module doesn't use them.
    // Using the stable wear-compose-material (not the alpha material3
    // for wear, which has not had a stable release at time of writing).
    implementation("androidx.wear.compose:compose-material:1.4.1")
    implementation("androidx.wear.compose:compose-foundation:1.4.1")
    implementation("androidx.wear:wear:1.3.0")

    debugImplementation(libs.androidx.compose.ui.tooling)
}

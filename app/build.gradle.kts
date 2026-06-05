plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.navitimerguide"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.navitimerguide"
        minSdk = 30
        targetSdk = 35
        versionCode = 14
        versionName = "1.0.6"

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
            // No release signing config: this app ships sideload-debug only.
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
        getByName("androidTest").java.srcDirs("src/androidTest/kotlin")
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // play-services-wearable transitively pulls androidx.fragment 1.1.0
    // (2019), which Google Play flags as an outdated SDK. The app never
    // uses fragments directly, so constrain the resolved version up to
    // current stable instead of adding a real dependency.
    constraints {
        implementation("androidx.fragment:fragment:1.8.9")
    }

    // The watch app is NOT embedded here. Modern Wear OS (2.0+) ignores
    // the legacy embedded micro-APK. The :wear module is built/installed
    // independently (sideload: adb install the wear APK straight to the
    // watch). Kept structurally parallel with the Slide Rule fork, which
    // ships the watch bundle via Play multi-APK form-factor delivery.

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.window)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.androidx.compose.material3.adaptive.layout)

    implementation(libs.kotlinx.datetime)

    // Bezel sync: phone↔watch Data Layer + persisted sync toggle.
    implementation(libs.play.services.wearable)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.play.services)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.junit)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.test.junit.ext)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}

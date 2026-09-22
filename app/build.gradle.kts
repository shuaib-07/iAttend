import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-kapt")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.dagger.hilt.android")
    id("com.mikepenz.aboutlibraries.plugin")
}

// Release signing: app/keystore.properties (gitignored, alongside the .jks it points at) - not
// committed, so a release build on a machine without it fails loudly here rather than silently
// falling back to debug signing again.
val keystorePropertiesFile = file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.iattend.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.iattend.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 7
        versionName = "0.3.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias", "iAttend")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }
    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

kapt {
    arguments {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
}

dependencies {
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material:material-icons-extended")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Dagger Hilt
    implementation("com.google.dagger:hilt-android:2.57")
    kapt("com.google.dagger:hilt-compiler:2.57")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // WorkManager (automated backup scheduling - periodic work doesn't need AlarmManager's exact-time precision)
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("androidx.hilt:hilt-work:1.2.0")
    kapt("androidx.hilt:hilt-compiler:1.2.0")

    // SAF folder access for user-chosen automated backup destinations
    implementation("androidx.documentfile:documentfile:1.0.1")

    // Kotlin Serialization (used for type-safe Nav Compose routes + JSON export/import)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.1")

    // Navigation Compose (type-safe routes)
    implementation("androidx.navigation:navigation-compose:2.9.0")

    // Room
    val roomVersion = "2.7.2"
    implementation("androidx.room:room-runtime:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    testImplementation("androidx.room:room-testing:$roomVersion")

    // DataStore (Settings + Profile)
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // QR code generation for the Developer Support UPI code
    implementation("com.google.zxing:core:3.5.3")

    // Squircle/superellipse corners (design.md) - Google's own smoothed-corner shape implementation.
    implementation("androidx.graphics:graphics-shapes:1.0.1")
    implementation(libs.haze)

    // Liquid-glass backdrop blur for the capsule nav bar style (Style B)
    implementation(libs.backdrop)

    // Async image loading for DiceBear avatar previews (Profile picker + Home greeting row)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // Animated nav bar / FAB icons (hover-morph Lottie files in app/src/main/assets/lottie)
    implementation(libs.lottie.compose)

    // Home screen widget (upcoming classes)
    implementation("androidx.glance:glance-appwidget:1.1.1")

    // Auto-generated open-source license list for the About screen (Settings)
    implementation("com.mikepenz:aboutlibraries-compose-m3:11.2.3")

    // Force a kotlin-metadata-jvm capable of reading backdrop's Kotlin-2.3-era metadata on the
    // kapt classpath - Room/Hilt's own kapt processors otherwise pull in an older transitive
    // version that can't parse it (independent of the project's own Kotlin compiler version).
    kapt("org.jetbrains.kotlin:kotlin-metadata-jvm:${libs.versions.kotlin.get()}")

    coreLibraryDesugaring(libs.android.desugar.jdk.libs)
}

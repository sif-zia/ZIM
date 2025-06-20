plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("kotlin-kapt") // Add this line
    id("com.google.dagger.hilt.android")
    kotlin("plugin.serialization") version "1.9.0"
}
kapt {
    correctErrorTypes = true // Ensure proper error handling for kapt
}
android {
    namespace = "com.example.zim"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.zim"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
        freeCompilerArgs = listOf("-Xjvm-default=all-compatibility")
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/INDEX.LIST" // Add this line
            excludes += "/META-INF/io.netty.versions.properties"
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.play.services.nearby)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // compose viewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.5.1")

    // Room
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    kapt ("androidx.room:room-compiler:$roomVersion")

    // Kotlin Extensions and Coroutines support for Room
    implementation("androidx.room:room-ktx:$roomVersion")

    // Coroutines
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.0")

    // Lifecycle components for coroutine support
    implementation ("androidx.lifecycle:lifecycle-viewmodel-ktx:2.5.1")
    implementation ("androidx.lifecycle:lifecycle-runtime-ktx:2.5.1")

    // Navigation
    implementation ("androidx.navigation:navigation-compose:2.7.1")

    // Dagger Hilt
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-android-compiler:2.48")
    implementation("androidx.hilt:hilt-navigation-compose:1.0.0")

    // Uri Loading Dependency (Coil)
    implementation("io.coil-kt:coil-compose:2.4.0")

    // Bottom Navigation
    implementation("androidx.navigation:navigation-compose:2.7.0-rc01")

    // Material Icons
    implementation("androidx.compose.material:material-icons-extended:1.7.5")

    //Fall detection
    implementation ("com.microsoft.onnxruntime:onnxruntime-android:1.16.0")
    implementation("org.apache.commons:commons-math3:3.6.1")

    //Ktor (API)
    implementation("io.ktor:ktor-server-core:2.3.8")
    implementation("io.ktor:ktor-server-netty:2.3.8")  // Netty engine
    implementation("io.ktor:ktor-server-content-negotiation:2.3.8")

    // Ktor Client Core Dependencies
    implementation("io.ktor:ktor-client-core:2.3.8")
    implementation("io.ktor:ktor-client-android:2.3.8")  // Android engine
    implementation("io.ktor:ktor-client-content-negotiation:2.3.8")

    // Shared Serialization
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.8")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

}
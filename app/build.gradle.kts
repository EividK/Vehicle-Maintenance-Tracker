plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.relay") version "0.3.12"

    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.vehiclehealth"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.vehiclehealth"
        minSdk = 24
        targetSdk = 35
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
    }
    buildFeatures {
        compose = true
        viewBinding = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/DEPENDENCIES"
        }
    }

}
configurations.all {
    resolutionStrategy {
        force("androidx.test.espresso:espresso-core:3.5.0")
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
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.database.ktx)
    implementation(libs.vision.common)
    implementation(libs.play.services.mlkit.text.recognition.common)
    implementation(libs.play.services.mlkit.text.recognition)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.ui.test.android)
    implementation(libs.androidx.work.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)


    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-analytics")

    // Maps Compose
    implementation ("com.google.maps.android:maps-compose:2.11.4")

    // Location & Maps
    implementation ("com.google.android.gms:play-services-maps:18.1.0")
    implementation ("com.google.android.gms:play-services-location:21.0.1")

    // Places SDK
    implementation ("com.google.android.libraries.places:places:3.4.0")

    implementation("com.google.firebase:firebase-auth-ktx")

    implementation("io.coil-kt:coil-compose:2.3.0")
    implementation("com.google.firebase:firebase-storage-ktx")

    implementation ("androidx.compose.material:material-icons-extended:1.5.0")

    // ML Kit for text recognition
    implementation("com.google.mlkit:text-recognition:16.0.0")

    // CameraX core libraries
    implementation ("androidx.camera:camera-core:1.2.0")
    implementation ("androidx.camera:camera-camera2:1.2.0")
    implementation ("androidx.camera:camera-lifecycle:1.2.0")
    implementation ("androidx.camera:camera-view:1.2.0")

    // Google Sign‑In and Calendar API
    implementation ("com.google.android.gms:play-services-auth:20.6.0")
    implementation ("com.google.api-client:google-api-client-android:1.34.1")
    implementation("com.google.apis:google-api-services-calendar:v3-rev411-1.25.0")

    // HTTP transport for Android
    implementation ("com.google.http-client:google-http-client-android:1.46.3")
    implementation ("com.google.http-client:google-http-client-gson:1.46.3")

    // Google API client libraries
    implementation("com.google.api-client:google-api-client-android:1.34.1")
    implementation("com.google.api-client:google-api-client-gson:1.34.1")

    // ComposeCalendar (by Bogusz Pawlowski)
    implementation("io.github.boguszpawlowski.composecalendar:composecalendar:1.4.0")

    // Firestore needs these gRPC artifacts at runtime
    implementation ("io.grpc:grpc-okhttp:1.68.0")
    implementation ("io.grpc:grpc-protobuf-lite:1.68.0")
    implementation ("io.grpc:grpc-stub:1.68.0")

    // Firebase Cloud Messaging
    implementation ("com.google.firebase:firebase-messaging-ktx:23.1.0")
    implementation ("com.google.firebase:firebase-functions-ktx:20.4.0")

    implementation ("androidx.compose.material3:material3-window-size-class:1.2.0")
    implementation ("com.google.firebase:firebase-auth-ktx:21.2.0")

    // React Native dependencies
    //implementation(project(":reactnative"))
    //implementation("com.facebook.react:react-native:+")

}
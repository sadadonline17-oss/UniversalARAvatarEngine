plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
    id("com.google.dagger.hilt.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.universalavatar.engine"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.universalavatar.engine"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0-Alpha"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // Native libraries for ML and graphics
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }

        externalNativeBuild {
            cmake {
                cppFlags += listOf("-O3", "-ffast-math", "-fopenmp")
                arguments += listOf(
                    "-DANDROID_STL=c++_shared",
                    "-DANDROID_CPP_FEATURES=rtti exceptions"
                )
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
            // Performance optimizations
            buildConfigField("boolean", "ENABLE_GPU_DELEGATE", "true")
            buildConfigField("boolean", "ENABLE_NNAPI", "true")
            buildConfigField("int", "TARGET_FPS", "30")
            buildConfigField("int", "TARGET_LATENCY_MS", "40")
        }
        debug {
            isDebuggable = true
            buildConfigField("boolean", "ENABLE_GPU_DELEGATE", "true")
            buildConfigField("boolean", "ENABLE_NNAPI", "true")
            buildConfigField("int", "TARGET_FPS", "30")
            buildConfigField("int", "TARGET_LATENCY_MS", "40")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api"
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
        viewBinding = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    packaging {
        resources {
            excludes += listOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/INDEX.LIST",
                "/META-INF/io.netty.versions.properties"
            )
        }
        jniLibs {
            useLegacyPackaging = true
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    // Asset packaging for ML models
    sourceSets["main"].assets {
        srcDirs("src/main/assets", "src/main/ml-models")
    }
}

dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-service:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    // Compose UI
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    // Dependency Injection
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-compiler:2.48")
    implementation("androidx.hilt:hilt-work:1.1.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // MediaPipe - Face Mesh (468 points)
    implementation("com.google.mediapipe:tasks-vision:0.10.8")
    implementation("com.google.mediapipe:solution-core:0.10.8")
    implementation("com.google.mediapipe:face-mesh:0.10.8")
    implementation("com.google.mediapipe:face-detection:0.10.8")

    // TensorFlow Lite - Deepfake Engine
    implementation("org.tensorflow:tensorflow-lite:2.14.0")
    implementation("org.tensorflow:tensorflow-lite-gpu:2.14.0")
    implementation("org.tensorflow:tensorflow-lite-gpu-api:2.14.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
    implementation("org.tensorflow:tensorflow-lite-metadata:0.4.4")
    implementation("org.tensorflow:tensorflow-lite-select-tf-ops:2.14.0")

    // NNAPI Delegate for hardware acceleration
    implementation("org.tensorflow:tensorflow-lite-delegates-nnapi:2.14.0")

    // GPU Delegate for OpenGL acceleration
    implementation("org.tensorflow:tensorflow-lite-delegates-gpu:2.14.0")

    // CameraX - Virtual Scene Composer
    val cameraxVersion = "1.3.1"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-video:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")
    implementation("androidx.camera:camera-mlkit-vision:$cameraxVersion")
    implementation("androidx.camera:camera-extensions:$cameraxVersion")

    // OpenGL / Graphics
    implementation("androidx.opengl:opengl:1.0.0")
    implementation("com.google.ar:core:1.41.0")

    // Unity AR Foundation (optional - for advanced AR)
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar"))))

    // WebRTC - Screen Share Bridge
    implementation("org.webrtc:google-webrtc:1.0.5")
    implementation("io.getstream:stream-webrtc-android:1.1.1")

    // FFmpeg for video processing
    implementation("com.arthenica:ffmpeg-kit-full:6.0-2")

    // Network & Streaming
    implementation("io.ktor:ktor-client-android:2.3.7")
    implementation("io.ktor:ktor-client-websockets:2.3.7")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")

    // JSON Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

    // DataStore for preferences
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("androidx.datastore:datastore:1.0.0")

    // WorkManager for background tasks
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Room for local database
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")

    // Image processing
    implementation("com.github.bumptech.glide:glide:4.16.0")
    kapt("com.github.bumptech.glide:compiler:4.16.0")
    implementation("com.github.bumptech.glide:compose:1.0.0-beta01")

    // GPU Image processing
    implementation("jp.co.cyberagent.android:gpuimage:2.1.0")

    // ExoPlayer for video preview
    implementation("androidx.media3:media3-exoplayer:1.2.1")
    implementation("androidx.media3:media3-ui:1.2.1")

    // Analytics (optional)
    implementation("com.google.firebase:firebase-analytics-ktx:21.5.0")

    // Logging
    implementation("com.jakewharton.timber:timber:5.0.1")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("io.mockk:mockk:1.13.8")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.02.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

kapt {
    correctErrorTypes = true
}

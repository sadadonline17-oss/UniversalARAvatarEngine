// Top-level build file for Universal AR Avatar Engine
// Digital Identity Operating System for Android

plugins {
    id("com.android.application") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.20" apply false
    id("org.jetbrains.kotlin.kapt") version "1.9.20" apply false
    id("com.google.dagger.hilt.android") version "2.48" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.20" apply false
}

buildscript {
    dependencies {
        classpath("com.google.gms:google-services:4.4.0")
        classpath("com.android.tools.build:gradle:8.2.0")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots") }
        flatDir {
            dirs("libs")
        }
    }
}

// Project-wide constants
extra["mediapipeVersion"] = "0.10.8"
extra["tensorflowVersion"] = "2.14.0"
extra["cameraxVersion"] = "1.3.1"
extra["composeVersion"] = "1.6.0"
extra["kotlinCoroutinesVersion"] = "1.7.3"
extra["webrtcVersion"] = "1.0.5"
extra["unityVersion"] = "2023.2.5f1"

// Performance targets
extra["targetLatencyMs"] = 40
extra["targetFps"] = 30
extra["minSdk"] = 26
extra["targetSdk"] = 34
extra["compileSdk"] = 34

tasks.register<Delete>("clean") {
    delete(rootProject.buildDir)
}

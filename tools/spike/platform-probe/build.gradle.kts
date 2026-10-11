import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application") version "8.13.2"
    id("org.jetbrains.kotlin.android") version "2.2.20"
}

android {
    namespace = "io.github.yiannosparanomos.undertone.spike"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.yiannosparanomos.undertone.spike"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "m1-probe"
    }

    buildTypes {
        release { isMinifyEnabled = false }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint { abortOnError = false }
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
    implementation("androidx.core:core-ktx:1.16.0")
}

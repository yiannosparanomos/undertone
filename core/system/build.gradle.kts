import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// The platform layer: notification listener, sound player, DND controller, boundary alarms and
// the watchdog. Everything Android-specific that feeds the engine or carries out its decisions.
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "io.github.yiannosparanomos.undertone.core.system"
    compileSdk =
        libs.versions.compileSdk
            .get()
            .toInt()

    defaultConfig {
        minSdk =
            libs.versions.minSdk
                .get()
                .toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    api(project(":core:domain"))
    api(project(":core:data"))

    implementation(libs.androidx.core.ktx)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    // core:data keeps Room and DataStore as implementation details, which is right for
    // production; the pipeline tests build a real database and settings store, so they need
    // them on the test classpath explicitly.
    androidTestImplementation(libs.room.runtime)
    androidTestImplementation(libs.datastore)
}

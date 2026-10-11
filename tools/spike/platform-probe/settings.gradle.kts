// Standalone build: deliberately NOT included in the repo's settings.gradle.kts, so the
// throwaway M1 probe can never end up in an UnderTone APK. Delete this folder after M1.
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "platform-probe"

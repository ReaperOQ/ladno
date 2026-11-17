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

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "ladno"
include(
    ":androidApp",
    ":notificationNotifier",
    ":webApp",
    ":shared:domain",
    ":shared:engine",
    ":shared:data",
    ":shared:ui",
    ":shared:designsystem",
    ":platform:android",
    ":server",
)

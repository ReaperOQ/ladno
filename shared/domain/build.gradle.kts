@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.multiplatform)
}

kotlin {
    android {
        namespace = "app.ladno.domain"
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk = libs.versions.minSdk.get().toInt()
        withHostTestBuilder { sourceSetTreeName = "test" }.configure { }
    }

    wasmJs {
        browser { testTask { useKarma { useChromeHeadless() } } }
    }

    sourceSets.commonMain.dependencies {
        api(libs.serialization.json)
        api(libs.datetime)
        api(libs.coroutines.core)
    }
    sourceSets.commonTest.dependencies { implementation(kotlin("test")) }
}

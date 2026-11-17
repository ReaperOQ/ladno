@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.multiplatform)
}

kotlin {
    android {
        namespace = "app.ladno.data"
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk = libs.versions.minSdk.get().toInt()
        withHostTestBuilder { sourceSetTreeName = "test" }.configure { }
        withDeviceTest {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    wasmJs {
        browser {
            testTask {
                useKarma { useChromeHeadless() }
            }
        }
    }

    sourceSets.commonMain.dependencies {
        implementation(projects.shared.domain)
        implementation(projects.shared.engine)
        implementation(libs.cryptography.core)
        implementation(libs.cryptography.random)
    }
    sourceSets.androidMain.dependencies { implementation(libs.cryptography.provider.jdk) }
    sourceSets.wasmJsMain.dependencies {
        implementation(libs.cryptography.provider.webcrypto)
        implementation(libs.coroutines.core)
    }
    sourceSets.commonTest.dependencies {
        implementation(kotlin("test"))
        implementation(libs.coroutines.test)
    }
    sourceSets.named("androidDeviceTest") {
        dependencies {
            implementation(kotlin("test-junit"))
            implementation(libs.androidx.test.runner)
        }
    }
}

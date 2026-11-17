plugins {
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.multiplatform) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.spring.boot) apply false
}

plugins.withType<org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnPlugin> {
    // datetime 0.7.1 requests core 3.2.0; timezone must share one core >= 5.7.0.
    the<org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnRootExtension>()
        .resolution("@js-joda/core", libs.versions.jsjodaCore.get())
}

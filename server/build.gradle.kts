plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.spring.boot)
}

kotlin {
    jvmToolchain(libs.versions.jdk.get().toInt())
}

dependencies {
    implementation(platform(libs.spring.boot.dependencies))
    implementation(libs.spring.boot.webmvc)
    implementation(libs.kotlin.reflect)
}

tasks.bootJar {
    archiveFileName.set("ladno-server.jar")
}

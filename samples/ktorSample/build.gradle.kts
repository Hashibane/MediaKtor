plugins {
    // Apply the shared build logic from a convention plugin.
    // The shared code is located in `buildSrc/src/main/kotlin/kotlin-jvm.gradle.kts`.
    id("buildsrc.convention.kotlin-jvm")
    id("com.google.devtools.ksp") version "2.3.11"
    alias(libs.plugins.kotlinPluginSerialization)

}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("com.hashibane:mediaktor-core:0.0.2")
    implementation("com.hashibane:mediaktor-koin:0.0.2")
    ksp("com.hashibane:mediaktor-koin:0.0.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.config.yaml)
    implementation(libs.logback.classic)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.ktor)
}
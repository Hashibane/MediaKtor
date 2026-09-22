plugins {
    id("buildsrc.convention.kotlin-jvm")
    alias(libs.plugins.ksp)
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
    implementation(libs.kotlinxCoroutines)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.config.yaml)
    implementation(libs.logback.classic)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.ktor)
}
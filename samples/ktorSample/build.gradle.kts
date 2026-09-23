plugins {
    id("buildsrc.convention.kotlin-jvm")
    id("com.google.devtools.ksp")
    alias(libs.plugins.kotlinPluginSerialization)

}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation(project(":projects:mediaktor-core"))
    implementation(project(":projects:mediaktor-koin"))
    ksp(project(":projects:mediaktor-koin"))
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
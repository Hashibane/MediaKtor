plugins {
    kotlin("jvm")
}

group = "com.hashibane"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation(project(":core"))
    implementation("com.google.devtools.ksp:symbol-processing-api:2.3.6")
    implementation("com.squareup:kotlinpoet:2.3.0")
    implementation("com.squareup:kotlinpoet-ksp:2.3.0")
    implementation(libs.ktor.server.di)
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}
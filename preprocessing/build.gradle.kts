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
    testImplementation("io.mockk:mockk-jvm:1.14.11")
    testImplementation(files("D:\\log4j\\slf4j-log4j13-1.0.1.jar"))

    implementation(project(":core"))
    implementation("com.google.devtools.ksp:symbol-processing-api:2.3.6")
    implementation("com.squareup:kotlinpoet:2.3.0")
    implementation("com.squareup:kotlinpoet-ksp:2.3.0")
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.ktor)
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}
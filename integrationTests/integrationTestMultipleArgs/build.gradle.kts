plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp") version "2.3.11"
}

group = "org.example"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    kspTest(project(":projects:mediaktor-koin"))
    testImplementation(project(":projects:mediaktor-koin"))
    testImplementation(project(":projects:mediaktor-core"))
    testImplementation(platform(libs.koin.bom))
    testImplementation("io.insert-koin:koin-core")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()

    testLogging {
        events("passed")
    }
}
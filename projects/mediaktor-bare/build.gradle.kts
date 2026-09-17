plugins {
    kotlin("jvm")
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("io.mockk:mockk-jvm:1.14.11")
    testImplementation("io.github.oshai:kotlin-logging-jvm:7.0.3")
    testImplementation(project(":projects:mediaktor-testing"))
    testImplementation(project(":projects:mediaktor-core"))

    implementation(project(":projects:mediaktor-core"))
    implementation("com.google.devtools.ksp:symbol-processing-api:2.3.6")
    implementation("com.squareup:kotlinpoet:2.3.0")
    implementation("com.squareup:kotlinpoet-ksp:2.3.0")
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
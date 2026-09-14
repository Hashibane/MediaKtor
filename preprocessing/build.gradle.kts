plugins {
    kotlin("jvm")
    `maven-publish`
}

group = "com.hashibane"
version = "0.0.0"

repositories {
    mavenCentral()
}

publishing {
    publications {
        create<MavenPublication>("MediaKtor") {
            groupId = "com.hashibane"
            artifactId = "MediaKtor"
            version = "0.0.0"

            from(components["kotlin"])
        }
    }
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("io.mockk:mockk-jvm:1.14.11")
    testImplementation("io.github.oshai:kotlin-logging-jvm:7.0.3")

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

    testLogging {
        events("passed")
    }
}
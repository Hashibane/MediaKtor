plugins {
    id("buildsrc.convention.publish")
    id("buildsrc.convention.kotlin-jvm")
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation(libs.mock)
    testImplementation("io.github.oshai:kotlin-logging-jvm:7.0.3")
    testImplementation(project(":projects:mediaktor-testing"))
    testImplementation(project(":projects:mediaktor-core"))

    implementation(project(":projects:mediaktor-core"))
    implementation(libs.ksp.api)
    implementation(libs.poet)
    implementation(libs.poet.ksp)
}
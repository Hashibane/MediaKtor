plugins {
    id("buildsrc.convention.publish")
    id("buildsrc.convention.kotlin-jvm")
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation(libs.mock)
    testImplementation(libs.logging)
    testImplementation(project(":projects:mediaktor-testing"))
    testImplementation(project(":projects:mediaktor-core"))

    implementation(project(":projects:mediaktor-core"))
    implementation(libs.ksp.api)
    implementation(libs.poet)
    implementation(libs.poet.ksp)
}
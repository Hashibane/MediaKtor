plugins {
    id("buildsrc.convention.kotlin-jvm")
}

dependencies {
    testImplementation(kotlin("test"))
    implementation(libs.mock)

    implementation(project(":projects:mediaktor-core"))
}
plugins {
    id("buildsrc.convention.testing")
}

dependencies {
    testImplementation(kotlin("test"))
    kspTest(project(":projects:mediaktor-bare"))

    testImplementation(project(":projects:mediaktor-core"))
    testImplementation(libs.kotlinxCoroutines)
}
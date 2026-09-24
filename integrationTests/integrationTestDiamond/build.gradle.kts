plugins {
    id("buildsrc.convention.testing")
}

dependencies {
    testImplementation(kotlin("test"))
    kspTest(project(":projects:mediaktor-koin"))
    testImplementation(project(":projects:mediaktor-koin"))
    testImplementation(project(":projects:mediaktor-core"))
    testImplementation(libs.kotlinxCoroutines)
    testImplementation(platform(libs.koin.bom))
    testImplementation(libs.koin.core)
}
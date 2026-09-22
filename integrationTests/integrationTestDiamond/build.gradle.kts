dependencies {
    testImplementation(kotlin("test"))
    kspTest(project(":projects:mediaktor-koin"))
    testImplementation(project(":projects:mediaktor-koin"))
    testImplementation(project(":projects:mediaktor-core"))
    testImplementation(platform(libs.koin.bom))
    testImplementation(libs.koin.core)
    testImplementation(libs.kotlinxCoroutines)
}
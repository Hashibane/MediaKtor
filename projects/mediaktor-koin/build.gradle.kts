plugins {
    id("buildsrc.convention.kotlin-jvm")
    id("buildsrc.convention.dokka")
    id("buildsrc.convention.codegen")
}


dependencies {
    testImplementation(kotlin("test"))
    testImplementation(libs.mock)
    testImplementation(project(":projects:mediaktor-testing"))
    testImplementation(project(":projects:mediaktor-core"))
    implementation(project(":projects:mediaktor-core"))
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
}
plugins {
    id("buildsrc.convention.publish")
    id("buildsrc.convention.kotlin-jvm")
}

dependencies {
    testImplementation(kotlin("test"))

    implementation(libs.ksp.api)
    implementation(libs.poet)
    implementation(libs.poet.ksp)
}
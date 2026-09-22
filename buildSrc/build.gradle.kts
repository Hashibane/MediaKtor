plugins {
    // The Kotlin DSL plugin provides a convenient way to develop convention plugins.
    // Convention plugins are located in `src/main/kotlin`, with the file extension `.gradle.kts`,
    // and are applied in the project's `build.gradle.kts` files as required.
    `kotlin-dsl`
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    // Add a dependency on the Kotlin Gradle plugin, so that convention plugins can apply it.
    implementation(libs.kotlinGradlePlugin)
    implementation(libs.kotlinJvm)
    implementation(libs.ksp.api)
    implementation(libs.poet)
    implementation(libs.poet.ksp)
    implementation(libs.dokka)
    implementation(libs.dokka.javadoc)
    implementation(libs.kover)
}

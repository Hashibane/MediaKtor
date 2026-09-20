plugins {
    alias(libs.plugins.kover)
}

subprojects {
    apply {
        plugin("org.jetbrains.kotlin.jvm")
        plugin("org.jetbrains.kotlinx.kover")
    }
}
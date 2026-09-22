plugins {
    id("buildsrc.convention.kotlin-jvm")
    alias(libs.plugins.ksp)
}

subprojects {
    apply {
        plugin("buildsrc.convention.kotlin-jvm")
        plugin("com.google.devtools.ksp")
    }

    dependencies {
        testImplementation(project(":projects:mediaktor-core"))
        testImplementation(rootProject.libs.kotlinxCoroutines)
    }
}
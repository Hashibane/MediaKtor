plugins {
    id("buildsrc.convention.publish")
    id("buildsrc.convention.kotlin-kmp")
}

kotlin {
    sourceSets {
        jvmMain.dependencies {
            implementation(project(":projects:mediaktor-core"))
            implementation(libs.ksp.api)
            implementation(libs.poet)
            implementation(libs.poet.ksp)
        }
        jvmTest.dependencies {
            implementation(libs.mock)
            implementation(libs.logging)
            implementation(libs.kotlin.test)
            implementation(project(":projects:mediaktor-testing"))
            implementation(project(":projects:mediaktor-core"))
        }
    }
}
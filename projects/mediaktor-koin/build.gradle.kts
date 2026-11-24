plugins {
    id("buildsrc.convention.publish")
    id("buildsrc.convention.kotlin-kmp")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":projects:mediaktor-core"))
            implementation(libs.koin.core)
        }
        jvmMain.dependencies {
            implementation(libs.ksp.api)
            implementation(libs.poet)
            implementation(libs.poet.ksp)
        }
        jvmTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.mock)
            implementation(project(":projects:mediaktor-testing"))
            implementation(project(":projects:mediaktor-core"))
        }
    }
}
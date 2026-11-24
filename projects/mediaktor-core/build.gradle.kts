plugins {
    id("buildsrc.convention.publish")
    id("buildsrc.convention.kotlin-kmp")
}

kotlin {
    sourceSets {
        commonMain.dependencies {}
        jvmMain.dependencies {
            implementation(libs.ksp.api)
            implementation(libs.poet)
            implementation(libs.poet.ksp)
        }
    }
}
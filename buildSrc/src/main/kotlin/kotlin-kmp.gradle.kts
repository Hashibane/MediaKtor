package buildsrc.convention

plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

kotlin {
    jvm {
        javaToolchains {
            version = 21
        }
    }
}
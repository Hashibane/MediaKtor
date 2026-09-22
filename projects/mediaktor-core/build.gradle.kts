plugins {
    id("buildsrc.convention.kotlin-jvm")
    id("buildsrc.convention.dokka")
    id("buildsrc.convention.codegen")
}

dependencies {
    testImplementation(kotlin("test"))
}
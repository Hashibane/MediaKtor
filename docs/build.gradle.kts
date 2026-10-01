plugins {
    id("buildsrc.convention.kotlin-jvm")
    id("org.jetbrains.dokka")
}

dependencies {
    dokka(project(":projects:mediaktor-bare"))
    dokka(project(":projects:mediaktor-core"))
    dokka(project(":projects:mediaktor-koin"))
}

dokka {
    moduleName.set("MediaKtor documentation")
}
import org.jetbrains.dokka.DokkaDefaults.moduleName

plugins {
    kotlin("jvm")
    alias(libs.plugins.dokka).apply(false)
    alias(libs.plugins.nmcp)
}

subprojects {
    group = rootProject.group
    version = rootProject.version

    apply {
        plugin("org.jetbrains.dokka")
        plugin("com.gradleup.nmcp")
        plugin("org.jetbrains.kotlin.jvm")
        plugin("maven-publish")
    }

    val dokkaHtml = tasks.named("dokkaGenerateHtml")

    val javadocJar: TaskProvider<Jar> by tasks.registering(Jar::class) {
        dependsOn(dokkaHtml)
        archiveClassifier.set("javadoc")
        from(layout.buildDirectory.dir("dokka/html"))
    }

    apply(from = rootProject.file("gradle/publish.gradle.kts"))
}
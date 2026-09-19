plugins {
    kotlin("jvm")
    alias(libs.plugins.dokka).apply(false)
    alias(libs.plugins.nmcp)
}

fun MutableSet<Project>.exclude(substring: String, body: Action<Project>) =
    this.filter { !it.name.contains(substring) }.forEach { body(it) }

subprojects.exclude("testing") {
    group = rootProject.group
    version = rootProject.version

    apply {
        plugin("org.jetbrains.dokka")
        plugin("com.gradleup.nmcp")
        plugin("org.jetbrains.kotlin.jvm")
        plugin("maven-publish")
        plugin("org.jetbrains.kotlinx.kover")
    }

    val dokkaHtml = tasks.named("dokkaGenerateHtml")

    val javadocJar: TaskProvider<Jar> by tasks.registering(Jar::class) {
        dependsOn(dokkaHtml)
        archiveClassifier.set("javadoc")
        from(layout.buildDirectory.dir("dokka/html"))
    }

    apply(from = rootProject.file("gradle/publish.gradle.kts"))
}
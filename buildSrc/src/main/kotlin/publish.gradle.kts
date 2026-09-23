package buildsrc.convention

import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SourcesJar


plugins {
    id("com.vanniktech.maven.publish")
    id("org.jetbrains.dokka")
    id("org.jetbrains.dokka-javadoc")
    id("org.jetbrains.kotlinx.kover")
}


mavenPublishing {
    configureBasedOnAppliedPlugins(
        javadocJar = JavadocJar.Dokka("dokkaGenerateJavadoc"),
        sourcesJar = SourcesJar.Sources(),
    )
}
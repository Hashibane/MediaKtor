package buildsrc.convention

import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SourcesJar
import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier


plugins {
    id("com.vanniktech.maven.publish")
    id("org.jetbrains.dokka")
    id("org.jetbrains.dokka-javadoc")
    id("org.jetbrains.kotlinx.kover")
}

dokka {
    dokkaSourceSets {
        configureEach {
            documentedVisibilities(VisibilityModifier.Public)
            perPackageOption {
                matchingRegex.set(".*preprocessing.*")
                suppress.set(true)
            }
        }
    }
}

mavenPublishing {
    configureBasedOnAppliedPlugins(
        javadocJar = JavadocJar.Dokka("dokkaGenerateJavadoc"),
        sourcesJar = SourcesJar.Sources(),
    )
}
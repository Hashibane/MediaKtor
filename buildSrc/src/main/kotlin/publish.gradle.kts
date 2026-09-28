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

    publishToMavenCentral()
    signAllPublications()

    coordinates(rootProject.group.toString(), project.name, rootProject.version.toString())

    pom {
        name = project.name
        description = "Concise mediator implementation in Kotlin"
        inceptionYear = "2026"
        url = "https://github.com/Hashibane/MediaKtor"
        licenses {
            license {
                name = "MIT License"
                url = "https://opensource.org/license/mit"
                distribution = "https://opensource.org/license/mit"
            }
        }
        developers {
            developer {
                id = "Hashibane"
                name = "Jacek Jeczeń"
                email = "jacek_jeczen@proton.me"

                organization = "Hashibane"
                organizationUrl = "https://github.com/Hashibane"
            }
        }
        scm {
            url = "https://github.com/Hashibane/MediaKtor"
            connection = "scm:git:git://github.com/Hashibane/MediaKtor.git"
            developerConnection = "scm:git:ssh://git@github.com/Hashibane/MediaKtor.git"
        }
    }
}
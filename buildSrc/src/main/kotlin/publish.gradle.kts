package buildsrc.convention

plugins {
    `maven-publish`
    `java-library`
    id("org.jetbrains.dokka")
    id("org.jetbrains.dokka-javadoc")
    id("org.jetbrains.kotlinx.kover")
}

dokka {
    // To generate documentation in Javadoc
    val dokkaJavadocJar by tasks.registering(Jar::class) {
        description = "A Javadoc JAR containing Dokka Javadoc"
        from(tasks.dokkaGeneratePublicationJavadoc.flatMap { it.outputDirectory })
        archiveClassifier.set("javadoc")
    }

    publishing {
        publications {
            create<MavenPublication>("maven") {
                from(components["java"])

                artifact(dokkaJavadocJar)

                artifactId = project.name
                groupId = rootProject.group.toString()
                version = rootProject.version.toString()

                pom {
                    name.set("MediaKtor")
                    description.set("MediaKtor - concise and unopinionated mediator project in Kotlin")
                    url.set("TODO")
                    licenses {
                        license {
                            name.set("TODO")
                            url.set("TODO")
                        }
                    }
                    scm {
                        url.set("TODO")
                        connection.set("TODO")
                    }
                    developers {
                        developer {
                            name.set("Jacek Jeczeń")
                            email.set("jacek_jeczen@proton.me")
                        }
                    }
                }
            }
        }
    }
}
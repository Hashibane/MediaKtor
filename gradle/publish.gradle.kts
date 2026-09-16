apply(plugin = "maven-publish")

val javadocJar = tasks.getByName("javadocJar")

configure<PublishingExtension> {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            artifact(javadocJar)

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
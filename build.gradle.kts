import buildsrc.convention.exclude

plugins {
    id("buildsrc.convention.kotlin-jvm")
    id("org.jetbrains.dokka")
    id("org.jetbrains.dokka-javadoc")
    id("org.jetbrains.kotlinx.kover")
}


dependencies {
    implementation(libs.dokka)

    subprojects.exclude("samples") {
        subprojects.exclude("testing") {
            this@dependencies.implementation(project)
            this@dependencies.kover(project)
        }
    }

    subprojects.exclude("samples", "Test") {
        subprojects.exclude("testing") {
            this@dependencies.dokka(project)
        }
    }
}


kover {
    reports {
        verify {
            rule {
                minBound(85)
            }
        }
    }
}
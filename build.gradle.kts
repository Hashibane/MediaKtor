import buildsrc.convention.exclude

plugins {
    id("buildsrc.convention.kotlin-jvm")
    id("org.jetbrains.kotlinx.kover")
}


dependencies {
    subprojects.exclude("samples") {
        subprojects.exclude("testing") {
            this@dependencies.implementation(project)
            this@dependencies.kover(project)
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
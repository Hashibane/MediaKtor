import buildsrc.convention.exclude

plugins {
    id("buildsrc.convention.kotlin-jvm")
    id("buildsrc.convention.dokka")
    id("org.jetbrains.kotlinx.kover")
    alias(libs.plugins.nmcp)
}

subprojects.exclude("samples", "test") {
    subprojects.exclude("testing") {
        apply {
            plugin("buildsrc.convention.core-plugin")
            plugin("buildsrc.convention.dokka")
            plugin("com.gradleup.nmcp")
        }
    }
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

nmcpAggregation {
    publishAllProjectsProbablyBreakingProjectIsolation()
}
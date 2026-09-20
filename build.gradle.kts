import buildsrc.convention.exclude

plugins {
    kotlin("jvm")
    alias(libs.plugins.nmcp)
    alias(libs.plugins.kover)
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

nmcpAggregation {
    publishAllProjectsProbablyBreakingProjectIsolation()
}
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        mavenLocal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include("projects:mediaktor-core")
include("projects:mediaktor-testing")
include("projects:mediaktor-bare")
include("projects:mediaktor-koin")

include("integrationTests:integrationTestSimple")
include("integrationTests:integrationTestMultipleArgs")
include("integrationTests:integrationTestDiamond")

include("samples:ktorSample")

rootProject.name = "MediaKtor"
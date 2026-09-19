plugins {
    alias(libs.plugins.nmcp)
    alias(libs.plugins.kover)
}

dependencies {
    kover(project(":projects:mediaktor-core"))
    kover(project(":projects:mediaktor-testing"))
    kover(project(":projects:mediaktor-bare"))
    kover(project(":projects:mediaktor-koin"))
    kover(project(":integrationTests:integrationTestDiamond"))
    kover(project(":integrationTests:integrationTestMultipleArgs"))
    kover(project(":integrationTests:integrationTestSimple"))
}

nmcpAggregation {
    publishAllProjectsProbablyBreakingProjectIsolation()
}
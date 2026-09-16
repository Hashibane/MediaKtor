plugins {
    alias(libs.plugins.nmcp)
}

nmcpAggregation {
    publishAllProjectsProbablyBreakingProjectIsolation()
}
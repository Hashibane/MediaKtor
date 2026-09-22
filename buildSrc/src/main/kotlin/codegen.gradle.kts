package buildsrc.convention

plugins {
    id("buildsrc.convention.kotlin-jvm")
}

dependencies {
    implementation("com.google.devtools.ksp:symbol-processing-api")
    implementation("com.squareup:kotlinpoet")
    implementation("com.squareup:kotlinpoet-ksp")
    testImplementation("com.google.devtools.ksp:symbol-processing-api")
    testImplementation("com.squareup:kotlinpoet")
    testImplementation("com.squareup:kotlinpoet-ksp")
}
package buildsrc.convention

plugins {
    id("buildsrc.convention.kotlin-jvm")
    id("buildsrc.convention.dokka")
    id("org.jetbrains.kotlinx.kover")
    `maven-publish`
}
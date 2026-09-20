package buildsrc.convention

import org.gradle.api.*
import org.gradle.kotlin.dsl.invoke

fun MutableSet<Project>.exclude(substring: String, body: Action<Project>) =
    this.filter { !it.name.contains(substring) }.forEach { body(it) }

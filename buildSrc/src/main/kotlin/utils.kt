package buildsrc.convention

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.kotlin.dsl.invoke

fun MutableSet<Project>.exclude(vararg substrings: String, body: Action<Project>) =
    this.filter { substrings.all { substring -> !it.name.contains(substring) } }.forEach { body(it) }

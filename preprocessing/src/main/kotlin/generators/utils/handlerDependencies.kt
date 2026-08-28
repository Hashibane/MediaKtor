package generators.utils

import com.google.devtools.ksp.processing.Dependencies
import metadata.HandlerMetadata

fun handlerDependencies(handlers: List<HandlerMetadata>): Dependencies {
    val sourceFiles = handlers.filter { it.origin != null }.map { it.origin!! }.toTypedArray()

    return if (sourceFiles.isNotEmpty())
        Dependencies(aggregating = true, *sourceFiles)
    else
        Dependencies.ALL_FILES
}
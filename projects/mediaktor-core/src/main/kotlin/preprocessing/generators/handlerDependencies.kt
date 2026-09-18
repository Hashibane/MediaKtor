package preprocessing.generators

import com.google.devtools.ksp.processing.Dependencies
import preprocessing.metadata.HandlerType

fun handlerDependencies(handlers: List<HandlerType>): Dependencies {
    val sourceFiles = handlers.filter { it.handlerMetadata.origin != null }.map { it.handlerMetadata.origin!! }.toTypedArray()

    return if (sourceFiles.isNotEmpty())
        Dependencies(aggregating = true, *sourceFiles)
    else
        Dependencies.ALL_FILES
}

fun handlerDependencies(handler: HandlerType): Dependencies {
    val sourceFile = handler.handlerMetadata.origin
    return if (sourceFile != null) {
        Dependencies(aggregating = false, sourceFile)
    } else {
        Dependencies.ALL_FILES
    }
}
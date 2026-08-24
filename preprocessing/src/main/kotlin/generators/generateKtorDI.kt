package generators

import com.squareup.kotlinpoet.FunSpec
import io.ktor.server.plugins.di.DependencyRegistry
import metadata.HandlerMetadata

fun generateKoin(handlers: List<HandlerMetadata>) {

    val configName = "provideMediator"

    val dependencyConfig = FunSpec.builder(configName)
        .receiver(DependencyRegistry::class)

    handlers.forEach {
        val className = it.generatedClass

        dependencyConfig.addStatement("")
    }
}
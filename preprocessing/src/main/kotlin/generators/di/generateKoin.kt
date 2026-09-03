package generators.di

import annotations.HandlerLifespan
import com.google.devtools.ksp.processing.CodeGenerator
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.MemberName.Companion.member
import com.squareup.kotlinpoet.ksp.writeTo
import generators.utils.handlerDependencies
import metadata.HandlerMetadata
import org.koin.core.module.Module

fun CodeGenerator.generateKtorDI(handlers: List<HandlerMetadata>, mediator: ClassName): FileSpec {

    val configName = "provideMediator"

    val dependencyConfig = FunSpec.builder(configName)
        .receiver(Module::class)

    val singleOf = MemberName("org.koin.core.module.dsl", "singleOf")
    val single = ClassName("org.koin.core.module", "Module")
        .member("single").simpleName
    handlers.forEach {
        val className = it.generatedClass

        val lifecycle = if (it.lifecycle == HandlerLifespan.SINGLE)
            singleOf
        else
            MemberName("org.koin.core.module.dsl", "factoryOf")

        dependencyConfig.addStatement("%M(%L)", lifecycle, className.constructorReference())
    }

    val mediatorInterface = MemberName("interfaces", "Mediator")
    val bind = MemberName("org.koin.dsl", "bind")
    val itName = "params"

    dependencyConfig.addStatement("%L { %N -> %T(${"{get()},".repeat(handlers.size)}) } %M %L::class",
        single, itName, mediator, bind, mediatorInterface)


    val fileSpec = FileSpec.builder("mediaktorKoin", "diSetup")
        .addFunction(dependencyConfig.build())
        .build()

    return fileSpec
}
package generators

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSFile
import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.ksp.writeTo
import processors.HandlerProcessor
/*
fun generateMediator(codeGenerator: CodeGenerator, handlers: List<TypeSpec, ClassName, KSFile>) {
    if (handlers.isEmpty()) return

    val mediatorClassName = "Mediator__Impl"
    val mediatorBuilder = TypeSpec.classBuilder(mediatorClassName)

    val constructorBuilder = FunSpec.constructorBuilder()
    handlers.forEach { (_, className, _) ->
        // Should be unique due to name generation
        val propName = className.simpleName.lowercase()

        val lambdaType = LambdaTypeName.get(returnType = className).copy(suspending = true)
        constructorBuilder.addParameter(propName, lambdaType)

        mediatorBuilder.addProperty(
            PropertySpec.builder(propName, lambdaType)
                .initializer(propName)
                .addModifiers(KModifier.PRIVATE)
                .build()
        )
    }

    mediatorBuilder.primaryConstructor(constructorBuilder.build())

    val parameterName = "command"

    val invokeBuilder = FunSpec.builder("invoke")
        .addModifiers(KModifier.SUSPEND)
        .addTypeVariable(TypeVariableName("T", Any::class))
        .addParameter(parameterName, TypeVariableName("T"))
        .returns(ANY.copy(nullable = true))
        .beginControlFlow("return when (%L)", parameterName)

    handlers.forEach { (typeName, className, _) ->
        val propName = className.simpleName.lowercase()

        invokeBuilder.beginControlFlow("is %T ->", typeName)
            .addStatement("%L().handleRequest(%L)", propName, parameterName)
            .endControlFlow()
    }


    val line = "throw IllegalArgumentException(\"No handler registered for command " + "$$parameterName" + "\")"
    invokeBuilder
        .addStatement("else -> %P", line)
        .endControlFlow()

    mediatorBuilder.addFunction(invokeBuilder.build())

    val fileSpec = FileSpec.builder(mediatorClassName, mediatorClassName)
        .addType(mediatorBuilder.build())
        .build()

    val sourceFiles = handlers.flatMap { (_, _, file) -> file }
    val namedSources = sourceFiles.toTypedArray()
    val dependencies = if (namedSources.isNotEmpty()) {
        Dependencies(aggregating = true, *namedSources)
    } else {
        Dependencies.ALL_FILES
    }

    fileSpec.writeTo(codeGenerator, dependencies)
}

 */
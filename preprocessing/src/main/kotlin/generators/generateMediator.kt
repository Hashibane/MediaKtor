package generators

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.writeTo
import metadata.HandlerMetadata

fun CodeGenerator.generateMediator(handlers: List<HandlerMetadata>) {
    if (handlers.isEmpty()) return

    val superInterface = ClassName("interfaces", "Mediator")

    val mediatorClassName = "Mediator__Impl"
    val mediatorBuilder = TypeSpec.classBuilder(mediatorClassName)
        .addSuperinterface(superInterface)

    val constructorBuilder = FunSpec.constructorBuilder()
    handlers.forEach {
        // Should be unique due to name generation
        val className = it.generatedClass
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
        .addModifiers(KModifier.SUSPEND, KModifier.OVERRIDE)
        .addTypeVariable(TypeVariableName("T", Any::class))
        .addParameter(parameterName, TypeVariableName("T"))
        .returns(ANY.copy(nullable = true))
        .beginControlFlow("return when (%L)", parameterName)

    handlers.forEach {
        val propName = it.generatedClass.simpleName.lowercase()

        invokeBuilder.beginControlFlow("is %T ->", it.inputType)
            .addStatement("%L().handleRequest(%L)", propName, parameterName)
            .endControlFlow()
    }


    val line = "No handler registered for command $$parameterName"
    invokeBuilder
        .addStatement("else -> throw IllegalArgumentException(%P)", line)
        .endControlFlow()

    mediatorBuilder.addFunction(invokeBuilder.build())

    val fileSpec = FileSpec.builder(mediatorClassName, mediatorClassName)
        .addType(mediatorBuilder.build())
        .build()

    val sourceFiles = handlers.filter { it.origin != null }.map { it.origin!! }.toTypedArray()
    val dependencies = if (sourceFiles.isNotEmpty()) {
        Dependencies(aggregating = true, *sourceFiles)
    } else {
        Dependencies.ALL_FILES
    }

    fileSpec.writeTo(this, dependencies)
}
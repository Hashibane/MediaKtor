package generators

import com.squareup.kotlinpoet.*
import metadata.HandlerMetadata
import kotlin.collections.forEach

data class MediatorMetadata(val className: ClassName, val fileSpec: FileSpec)

fun generateMediator(handlers: List<HandlerMetadata>): MediatorMetadata? {
    if (handlers.isEmpty()) return null

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
        .addModifiers(KModifier.SUSPEND, KModifier.OVERRIDE, KModifier.OPERATOR)
        .addTypeVariable(TypeVariableName("T", Any::class))
        .addParameter(parameterName, TypeVariableName("T"))
        .returns(ANY.copy(nullable = true))
        .beginControlFlow("return when (%L)", parameterName)

    handlers.filter { !it.isNotificationHandler } .forEach {
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

    val notificationHandlerMap = mutableMapOf<TypeName, MutableList<String>>()
    handlers.filter { it.isNotificationHandler }.forEach {
        if (notificationHandlerMap[it.inputType] == null) {
            notificationHandlerMap[it.inputType] = mutableListOf()
        }

        notificationHandlerMap[it.inputType]?.add(it.generatedClass.simpleName.lowercase())
    }

    val publishBuilder = FunSpec.builder("publish")
        .addModifiers(KModifier.SUSPEND, KModifier.OVERRIDE)
        .addTypeVariable(TypeVariableName("T", Any::class))
        .addParameter(parameterName, TypeVariableName("T"))
        .beginControlFlow("when (%L)", parameterName)

    notificationHandlerMap.forEach { (typeName, handlers) ->
        publishBuilder.beginControlFlow("is %T ->", typeName)

        handlers.forEach {
            publishBuilder.addStatement("%L().handleRequest(%L)", it, parameterName)
        }

        publishBuilder.endControlFlow()
    }

    val notificationLine = "No handler registered for notification $$parameterName"
    publishBuilder
        .addStatement("else -> throw IllegalArgumentException(%P)", notificationLine)
        .endControlFlow()

    mediatorBuilder.addFunction(publishBuilder.build())

    val mediatorClass = mediatorBuilder.build()
    val fileSpec = FileSpec.builder(mediatorClassName, mediatorClassName)
        .addType(mediatorClass)
        .build()

    return MediatorMetadata(
        ClassName(mediatorClassName, mediatorClassName),
        fileSpec
    )
}
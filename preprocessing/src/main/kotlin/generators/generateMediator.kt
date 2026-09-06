package generators

import annotations.NotificationParallel
import com.squareup.kotlinpoet.*
import metadata.HandlerMetadata
import metadata.HandlerType
import metadata.NotificationHandler
import metadata.NotificationHandlerMetadata
import metadata.RequestHandler
import kotlin.collections.forEach

data class MediatorMetadata(val className: ClassName, val fileSpec: FileSpec)
data class NotificationGenerationData(val name: String, val order: Int, val isParallel: Boolean)

fun generateMediator(handlerRegistry: MutableMap<TypeName, MutableList<HandlerType>>): MediatorMetadata? {
    if (handlerRegistry.isEmpty()) return null

    val superInterface = ClassName("interfaces", "Mediator")

    val mediatorClassName = "Mediator__Impl"
    val mediatorBuilder = TypeSpec.classBuilder(mediatorClassName)
        .addSuperinterface(superInterface)

    val constructorBuilder = FunSpec.constructorBuilder()
    handlerRegistry.forEach { (_, handlers) ->
        handlers.forEach {
            // Should be unique due to name generation
            val className = it.handlerMetadata.generatedClass
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
    }

    // TODO : write a comparator for two types so that it checks if one is child of another

    mediatorBuilder.primaryConstructor(constructorBuilder.build())

    val parameterName = "command"

    val invokeBuilder = FunSpec.builder("invoke")
        .addModifiers(KModifier.SUSPEND, KModifier.OVERRIDE, KModifier.OPERATOR)
        .addTypeVariable(TypeVariableName("T", Any::class))
        .addParameter(parameterName, TypeVariableName("T"))
        .returns(ANY.copy(nullable = true))
        .beginControlFlow("return when (%L)", parameterName)

    handlers.filter { it is RequestHandler }.forEach {
        val propName = it.handlerMetadata.generatedClass.simpleName.lowercase()
        invokeBuilder.beginControlFlow("is %T ->", it.handlerMetadata.inputType)
            .addStatement("%L().handleRequest(%L)", propName, parameterName)
            .endControlFlow()
    }


    val line = "No handler registered for command $$parameterName"
    invokeBuilder
        .addStatement("else -> throw IllegalArgumentException(%P)", line)
        .endControlFlow()

    mediatorBuilder.addFunction(invokeBuilder.build())

    val notificationHandlerMap = mutableMapOf<TypeName, MutableList<NotificationGenerationData>>()
    handlers.filterIsInstance<NotificationHandler>().forEach {
        if (notificationHandlerMap[it.handlerMetadata.inputType] == null) {
            notificationHandlerMap[it.handlerMetadata.inputType] = mutableListOf()
        }

        notificationHandlerMap[it.handlerMetadata.inputType]?.add(
            NotificationGenerationData(
                it.handlerMetadata.generatedClass.simpleName.lowercase(),
                it.notificationMetadata.order,
                it.notificationMetadata.parallel == NotificationParallel.PARALLEL
            )
        )
    }

    val publishBuilder = FunSpec.builder("publish")
        .addModifiers(KModifier.SUSPEND, KModifier.OVERRIDE)
        .addTypeVariable(TypeVariableName("T", Any::class))
        .addParameter(parameterName, TypeVariableName("T"))
        .beginControlFlow("when (%L)", parameterName)

    val coroutineScope = MemberName("kotlinx.coroutines", "coroutineScope")
    val launch = MemberName("kotlinx.coroutines", "launch")

    notificationHandlerMap.forEach { (typeName, handlers) ->
        publishBuilder.beginControlFlow("is %T ->", typeName)
        publishBuilder.beginControlFlow("%M", coroutineScope)

        handlers.sortedBy { it.order }.forEach {
            if (it.isParallel) {
                publishBuilder.beginControlFlow("%M", launch)
            }
            publishBuilder.addStatement("%L().handleRequest(%L)", it.name, parameterName)
            if (it.isParallel) {
                publishBuilder.endControlFlow()
            }
        }

        publishBuilder.endControlFlow().endControlFlow()
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
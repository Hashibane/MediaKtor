package generators

import annotations.NotificationParallel
import com.squareup.kotlinpoet.*
import errors.PreprocessingException
import metadata.HandlerType
import metadata.NotificationHandler
import metadata.PipelineHandler
import metadata.RequestHandler
import kotlin.collections.forEach

data class MediatorMetadata(val className: ClassName, val fileSpec: FileSpec)
data class NotificationGenerationData(val name: String, val order: Int, val isParallel: Boolean)

fun generateMediator(handlerRegistry: MutableMap<TypeName, MutableList<HandlerType>>,
                     typeSorter: Comparator<TypeName>): MediatorMetadata? {
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

    mediatorBuilder.primaryConstructor(constructorBuilder.build())

    val parameterName = "command"

    val invokeBuilder = FunSpec.builder("invoke")
        .addModifiers(KModifier.SUSPEND, KModifier.OVERRIDE, KModifier.OPERATOR)
        .addTypeVariable(TypeVariableName("T", Any::class))
        .addParameter(parameterName, TypeVariableName("T"))
        .returns(ANY.copy(nullable = true))
        .beginControlFlow("return when (%L)", parameterName)

    val publishBuilder = FunSpec.builder("publish")
        .addModifiers(KModifier.SUSPEND, KModifier.OVERRIDE)
        .addTypeVariable(TypeVariableName("T", Any::class))
        .addParameter(parameterName, TypeVariableName("T"))
        .beginControlFlow("when (%L)", parameterName)

    val sortedRegistry = handlerRegistry.toSortedMap(typeSorter)

    val coroutineScope = MemberName("kotlinx.coroutines", "coroutineScope")
    val launch = MemberName("kotlinx.coroutines", "launch")

    val pipelineCache: MutableMap<TypeName, MutableList<PipelineHandler>> = mutableMapOf()

    // TODO : can reduce from Theta(n^2)? We refilter PipelineHandlers each iteration
    // Don't think it's a problem for now, since n is small
    sortedRegistry.forEach { (type, handlers) ->
        val pipelines = handlers.filterIsInstance<PipelineHandler>().toMutableList()

        sortedRegistry
            .filter { (candidateType, _) -> typeSorter.compare(candidateType, type) > 0 }
            .forEach { (_, actualHandlers) ->
                pipelines.addAll(actualHandlers.filterIsInstance<PipelineHandler>())
            }
        pipelineCache[type] = pipelines.sortedBy { it.pipelineMetadata.order }.toMutableList()
    }

    sortedRegistry.forEach { (type, handlers) ->
        val requestHandlers = handlers.filterIsInstance<RequestHandler>()
        if (requestHandlers.isNotEmpty())
            invokeBuilder.beginControlFlow("is %T ->", type)

        val notificationHandlers = handlers.filterIsInstance<NotificationHandler>()
        if (notificationHandlers.isNotEmpty()) {
            publishBuilder.beginControlFlow("is %T ->", type)
            publishBuilder.beginControlFlow("%M", coroutineScope)
        }

        var unskipped = 0

        requestHandlers.forEach {
            for (handler in pipelineCache[type]!!) {
                val handlerReturn = it.handlerMetadata.returnType
                val pipelineReturn = handler.handlerMetadata.returnType

                // UNIT is valid for notification handlers.
                if (pipelineReturn != handlerReturn && pipelineReturn != UNIT && handlerReturn != UNIT) {
                    throw PreprocessingException("All pipelines must return type of the corresponding request handler: " +
                            "${it.handlerMetadata.generatedClass.simpleName}. " +
                            "Expected type: $handlerReturn, current type: $pipelineReturn")
                }

                // if not, then handlerReturn is UNIT and we should skip it
                if (pipelineReturn == handlerReturn) {
                    val propName = handler.handlerMetadata.generatedClass.simpleName.lowercase()
                    invokeBuilder.beginControlFlow("%L().handleRequest(%L)", propName, parameterName)

                    unskipped += 1
                }
            }

            val propName = it.handlerMetadata.generatedClass.simpleName.lowercase()
            invokeBuilder.addStatement("%L().handleRequest(%L)", propName, parameterName)

            // + 1 for "is %T"
            repeat (unskipped + 1) {
                invokeBuilder.endControlFlow()
            }
        }

        unskipped = 0

        if (notificationHandlers.isNotEmpty()) {
            for (handler in pipelineCache[type]!!) {
                if (handler.handlerMetadata.returnType != UNIT)
                    continue

                val propName = handler.handlerMetadata.generatedClass.simpleName.lowercase()
                publishBuilder.beginControlFlow("%L().handleRequest(%L)", propName, parameterName)

                unskipped += 1
            }

            notificationHandlers.sortedBy { it.notificationMetadata.order }.forEach {
                val propName = it.handlerMetadata.generatedClass.simpleName.lowercase()
                val isParallel = it.notificationMetadata.parallel == NotificationParallel.PARALLEL
                if (isParallel) {
                    publishBuilder.beginControlFlow("%M", launch)
                }
                publishBuilder.addStatement("%L().handleRequest(%L)", propName, parameterName)
                if (isParallel) {
                    publishBuilder.endControlFlow()
                }
            }

            // + 2 for coroutine scope and "is %T ->"
            repeat (unskipped + 2) {
                publishBuilder.endControlFlow()
            }
        }
    }

    val requestLine = "No handler registered for command $$parameterName"
    invokeBuilder
        .addStatement("else -> throw IllegalArgumentException(%P)", requestLine)
        .endControlFlow()

    mediatorBuilder.addFunction(invokeBuilder.build())

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
package generators

import annotations.NotificationParallel
import annotations.PipelineTarget
import com.google.devtools.ksp.processing.KSPLogger
import com.squareup.kotlinpoet.*
import metadata.HandlerType
import metadata.NotificationHandler
import metadata.PipelineHandler
import metadata.RequestHandler
import kotlin.collections.forEach

data class MediatorMetadata(val className: ClassName, val fileSpec: FileSpec)

fun generateMediator(handlerRegistry: MutableMap<TypeName, MutableList<HandlerType>>,
                     typeSorter: Comparator<TypeName>, logger: KSPLogger
): MediatorMetadata? {
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

    val requestParameterName = "message"
    val notificationParameterName = "notification"

    val invokeBuilder = FunSpec.builder("send")
        .addModifiers(KModifier.SUSPEND, KModifier.OVERRIDE)
        .addParameter(requestParameterName, ANY.copy(nullable = true))
        .returns(ANY.copy(nullable = true))
        .beginControlFlow("return when (%L)", requestParameterName)

    val publishBuilder = FunSpec.builder("publish")
        .addModifiers(KModifier.SUSPEND, KModifier.OVERRIDE)
        .addParameter(notificationParameterName, ANY.copy(nullable = true))
        .beginControlFlow("when (%L)", notificationParameterName)

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

        val strictPipes = pipelines.filter { it.pipelineMetadata.target.isStrict }
            .sortedBy { it.pipelineMetadata.order }
        val allPipes = pipelines.filter { !it.pipelineMetadata.target.isStrict }
            .sortedBy { it.pipelineMetadata.order }.toMutableList()
        allPipes.addAll(strictPipes)
        pipelineCache[type] = allPipes
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

        requestHandlers.forEach { requestHandler ->
            for (handler in pipelineCache[type]!!.filter {
                it.pipelineMetadata.target == PipelineTarget.STRICT_REQUESTS ||
                        it.pipelineMetadata.target == PipelineTarget.STRICT_BOTH ||
                        it.pipelineMetadata.target == PipelineTarget.PASS_REQUESTS ||
                        it.pipelineMetadata.target == PipelineTarget.PASS_BOTH
            }) {
                val handlerReturn = requestHandler.handlerMetadata.returnType

                val pipelineReturn = handler.handlerMetadata.returnType
                val isStrict = handler.pipelineMetadata.target.isStrict

                // We don't want incorrect program after adding a handler to responds to some child type and returns some
                // arbitrary type, which would not be the same as handler with parent type.
                if (pipelineReturn != handlerReturn && isStrict) {
                    logger.info("Pipeline ${handler.handlerMetadata.generatedClass.simpleName} was not applied to " +
                            "handler ${requestHandler.handlerMetadata.generatedClass.simpleName} because the return types do not match." +
                            "Pipeline return type: $pipelineReturn. Handler type: $handlerReturn")
                } else {
                    val propName = handler.handlerMetadata.generatedClass.simpleName.lowercase()
                    invokeBuilder.beginControlFlow("%L().handleRequest(%L)", propName, requestParameterName)

                    unskipped += 1
                }
            }

            val propName = requestHandler.handlerMetadata.generatedClass.simpleName.lowercase()
            invokeBuilder.addStatement("%L().handleRequest(%L)", propName, requestParameterName)

            // + 1 for "is %T"
            repeat (unskipped + 1) {
                invokeBuilder.endControlFlow()
            }
        }

        unskipped = 0

        if (notificationHandlers.isNotEmpty()) {
            for (handler in pipelineCache[type]!!.filter {
                it.pipelineMetadata.target == PipelineTarget.STRICT_NOTIFICATIONS ||
                        it.pipelineMetadata.target == PipelineTarget.STRICT_BOTH ||
                        it.pipelineMetadata.target == PipelineTarget.PASS_NOTIFICATIONS ||
                        it.pipelineMetadata.target == PipelineTarget.PASS_BOTH
            }) {
                val propName = handler.handlerMetadata.generatedClass.simpleName.lowercase()
                publishBuilder.beginControlFlow("%L().handleRequest(%L)", propName, notificationParameterName)

                unskipped += 1
            }

            notificationHandlers.sortedBy { it.notificationMetadata.order }.forEach {
                val propName = it.handlerMetadata.generatedClass.simpleName.lowercase()
                val isParallel = it.notificationMetadata.parallel == NotificationParallel.PARALLEL
                if (isParallel) {
                    publishBuilder.beginControlFlow("%M", launch)
                }
                publishBuilder.addStatement("%L().handleRequest(%L)", propName, notificationParameterName)
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

    val requestLine = "No handler registered for command $$requestParameterName"
    invokeBuilder
        .addStatement("else -> throw IllegalArgumentException(%P)", requestLine)
        .endControlFlow()

    mediatorBuilder.addFunction(invokeBuilder.build())

    val notificationLine = "No handler registered for notification $$notificationParameterName"
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
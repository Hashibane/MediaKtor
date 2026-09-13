package processors

import annotations.HandlerLifespan
import annotations.NotificationParallel
import annotations.PipelineBehavior
import annotations.PipelineTarget
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Nullability
import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.toTypeVariableName
import errors.PreprocessingException
import metadata.HandlerDescriptor
import metadata.HandlerMetadata
import metadata.HandlerType
import metadata.NotificationHandler
import metadata.NotificationHandlerMetadata
import metadata.PipelineHandler
import metadata.PipelineMetadata
import metadata.RequestHandler


fun KSType.toTypeNameOrLambda(): TypeName {
    if (declaration is KSFunctionDeclaration) {
        return LambdaTypeName.get(
            parameters = (declaration as KSFunctionDeclaration).parameters
                .map { ParameterSpec.builder(it.name?.asString() ?: "_", TypeCache[it.type].toTypeName()).build() },
            returnType = (declaration as KSFunctionDeclaration).returnType!!.toTypeName(),
        )
    }
    else
        return toTypeName()
}

fun <T, R> MutableMap<T, MutableList<R>>.extend(key: T, element: R) {
    val list = get(key) ?: mutableListOf()
    list.add(element)
    this[key] = list
}

fun MutableMap<TypeName, MutableList<HandlerType>>.verifyMetadata() {
    forEach { (name, metadata) ->
        val filteredMetadata = metadata.filterIsInstance<RequestHandler>()
        if (filteredMetadata.size > 1) {
            throw PreprocessingException("The input types must be unique for each request handler. Found request" +
                    "type $name for generated classes ${filteredMetadata.first().handlerMetadata.generatedClass.simpleName} and ${
                        filteredMetadata.drop(1).first().handlerMetadata.generatedClass.simpleName}")
        }
    }
}

fun addMetadata(handlerRegistry: MutableMap<TypeName, MutableList<HandlerType>>,
                function: KSFunctionDeclaration, id: Int,
                handlerDescriptor: HandlerDescriptor,
                typeMetadata: MutableMap<TypeName, MutableList<TypeName>>) {
    val functionName = function.simpleName.asString()

    val requestArg = function.parameters.firstOrNull()
        ?: throw PreprocessingException("Handler $functionName must have at least one argument - the request. " +
                "Pass argument of type Unit if no arguments are needed.")

    val resolvedRequestArg = TypeCache[requestArg.type]
    if (resolvedRequestArg.arguments.isNotEmpty()) {
        throw PreprocessingException("Type $resolvedRequestArg of " +
                "function $functionName cannot be parametrized by other types.")
    }

    val resolvedClass = resolvedRequestArg.declaration as? KSClassDeclaration

    if (resolvedRequestArg.nullability == Nullability.NULLABLE) {
        throw PreprocessingException("Nullable types are not allowed as request types." +
                "The request type of handler $functionName must not be null and is ${resolvedRequestArg.toTypeName()}")
    }

    resolvedClass?.superTypes?.forEach {
        typeMetadata.extend(resolvedRequestArg.toTypeName(), TypeCache[it].toTypeName())
    }

    typeMetadata.extend(resolvedRequestArg.toTypeName(), ANY)

    val args = function.parameters.drop(1)

    val returnType = function.returnType ?: throw PreprocessingException("Error occured during the resolution" +
            "of return type of handler $functionName.")

    if (handlerDescriptor == HandlerDescriptor.NOTIFICATION_HANDLER) {
        val declaration = TypeCache[returnType].declaration
        if (declaration.qualifiedName!!.asString() != "kotlin.Unit") {
            throw PreprocessingException("Notification handler should return the Unit type. " +
                    "Caused by function $functionName")
        }
    }

    val packageName = function.packageName.asString()

    val annotationClassName = when (handlerDescriptor) {
        HandlerDescriptor.REQUEST_HANDLER -> annotations.RequestHandler::class.simpleName!!
        HandlerDescriptor.NOTIFICATION_HANDLER -> annotations.NotificationHandler::class.simpleName!!
        HandlerDescriptor.PIPELINE_HANDLER -> PipelineBehavior::class.simpleName!!
    }

    val functionAnnotationArgs = function.annotations
        .find { it.shortName.asString() == annotationClassName }
        ?.arguments

    val handlerMetadata = HandlerMetadata(
        MemberName(packageName, functionName),
        generatedClass = ClassName(packageName, "Handler__${functionName}__$id"),
        inputType = resolvedRequestArg.toTypeName(),
        args = args.map {
            val propName = it.name?.asString()!! // cannot be null
            val typeName = TypeCache[it.type].toTypeNameOrLambda()

            ParameterSpec.builder(propName, typeName).build()
        },
        returnType = TypeCache[returnType].toTypeName(),
        origin = function.containingFile,
        lifecycle = when (val lifecycle = functionAnnotationArgs?.find { it.name?.asString() == "lifespan" }?.value.toString()) {
            "HandlerLifespan.SINGLE" -> HandlerLifespan.SINGLE
            "HandlerLifespan.FACTORY" -> HandlerLifespan.FACTORY
            else -> throw PreprocessingException(
                "Unknown lifecycle specifier $lifecycle on" +
                        " handler function $functionName. Expected SINGLE or FACTORY"
            )
        }
    )

    when (handlerDescriptor) {
        HandlerDescriptor.REQUEST_HANDLER -> handlerRegistry.extend(resolvedRequestArg.toTypeName(), RequestHandler(handlerMetadata))
        HandlerDescriptor.NOTIFICATION_HANDLER -> {
            val parallel = functionAnnotationArgs?.find { it.name?.asString() == "parallel" }?.value.toString()
            val order = functionAnnotationArgs?.find { it.name?.asString() == "order" }?.value.toString()

            val notificationMetadata = NotificationHandlerMetadata(
                when (parallel) {
                    "NotificationParallel.SEQUENTIAL" -> NotificationParallel.SEQUENTIAL
                    "NotificationParallel.PARALLEL" -> NotificationParallel.PARALLEL
                    else -> throw PreprocessingException(
                        "Unknown parallel specifier $parallel on" +
                                " notification function $functionName. Expected SEQUENTIAL or PARALLEL"
                    )
                },
                order.toInt()
            )
            handlerRegistry.extend(resolvedRequestArg.toTypeName(), NotificationHandler(handlerMetadata, notificationMetadata))
        }
        HandlerDescriptor.PIPELINE_HANDLER -> {
            val order = functionAnnotationArgs?.find { it.name?.asString() == "order" }?.value.toString()
            val target = functionAnnotationArgs?.find { it.name?.asString() == "target" }?.value.toString()
            val nextTypeCandidate = function.parameters.find { it.name?.asString() == "next" }?.type

            if (nextTypeCandidate == null)
                throw PreprocessingException("Pipeline handler $functionName should have one argument named \"next\" of type: " +
                        "(${requestArg.name?.asString()}) -> <HandlerOutputType>")

            val nextType = TypeCache[nextTypeCandidate]
            if (nextType.isSuspendFunctionType) {
                if (nextType.arguments.size > 1) {
                    throw PreprocessingException("The \"next\" argument of pipeline handler $functionName should be of type:\n" +
                            "() -> ${handlerMetadata.returnType}, found type: ${nextType.toTypeName()}")
                }

                val nextReturnType = nextType.arguments.firstOrNull()?.toTypeName()

                if (nextReturnType == null)
                    throw PreprocessingException("There was an error during resolution of return type $nextReturnType for " +
                            "handler $functionName of \"next\" argument")


                val pipelineTarget = when (target) {
                    "PipelineTarget.STRICT_REQUESTS" -> PipelineTarget.STRICT_REQUESTS
                    "PipelineTarget.STRICT_NOTIFICATIONS" -> PipelineTarget.STRICT_NOTIFICATIONS
                    "PipelineTarget.STRICT_BOTH" -> PipelineTarget.STRICT_BOTH
                    "PipelineTarget.PASS_REQUESTS" -> PipelineTarget.PASS_REQUESTS
                    "PipelineTarget.PASS_NOTIFICATIONS" -> PipelineTarget.PASS_NOTIFICATIONS
                    "PipelineTarget.PASS_BOTH" -> PipelineTarget.PASS_BOTH
                    else -> throw PreprocessingException(
                        "Unknown target specifier $target on" +
                                " pipeline function $functionName. Expected REQUESTS, NOTIFICATIONS or BOTH.")
                }

                verifyPipeline(
                    functionName = functionName,
                    nextReturn = nextReturnType,
                    returnType = handlerMetadata.returnType,
                    target = pipelineTarget
                )

                val pipelineMetadata = PipelineMetadata(nextReturnType,
                    order.toInt(),
                    pipelineTarget
                )
                handlerRegistry.extend(resolvedRequestArg.toTypeName(), PipelineHandler(handlerMetadata, pipelineMetadata))
            } else {
                throw PreprocessingException("Pipeline handler $functionName argument next should be of type:" +
                        " suspend () -> ${handlerMetadata.returnType} and is of type $nextType (maybe you need suspend?)")
            }
        }
    }
}
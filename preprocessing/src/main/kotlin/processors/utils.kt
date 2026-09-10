package processors

import annotations.HandlerLifespan
import annotations.NotificationParallel
import annotations.PipelineBehavior
import annotations.PipelineTarget
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Nullability
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.ksp.toClassName
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

    if (resolvedClass != null && resolvedRequestArg.nullability == Nullability.NOT_NULL) {
        val typename = resolvedRequestArg.toTypeName()
        typeMetadata.extend(typename, typename.copy(nullable = true))
    }
    resolvedClass?.superTypes?.forEach {
        if (resolvedRequestArg.nullability == Nullability.NOT_NULL)
            typeMetadata.extend(resolvedRequestArg.toTypeName(), TypeCache[it].toTypeName())
        typeMetadata.extend(resolvedRequestArg.toTypeName(), TypeCache[it].toTypeName().copy(nullable = true))
    }


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
                val requestType = nextType.arguments.firstOrNull()

                if (requestType == null) {
                    val isNullable = if (TypeCache[requestArg.type].nullability == Nullability.NULLABLE) "?" else ""
                    throw PreprocessingException(
                    "The expected type of the \"next\" argument of handler $functionName is: " +
                            "suspend (${TypeCache[requestArg.type].declaration.qualifiedName?.asString()}${isNullable}) -> <HandlerOutputType>"
                )}

                if (requestType.toTypeName() != resolvedRequestArg.toTypeName()) {
                    val isNullalbe = if (resolvedRequestArg.nullability == Nullability.NULLABLE) "?" else ""
                    throw PreprocessingException("The request type must be the same as type of \"next\" parameter." +
                            " Expected type suspend (${TypeCache[requestArg.type].declaration.simpleName.asString()}$isNullalbe) ->" +
                            " ${TypeCache[returnType].declaration.simpleName.asString()} on handler $functionName")
                }


                val returnType = nextType.declaration.typeParameters.drop(1).firstOrNull()

                if (returnType == null)
                    throw PreprocessingException("There was an error during resolution of return type $returnType for " +
                            "handler $functionName of \"next\" argument")

                val pipelineMetadata = PipelineMetadata(returnType.toTypeVariableName(),
                    order.toInt(),
                    when (target) {
                    "PipelineTarget.REQUESTS" -> PipelineTarget.REQUESTS
                    "PipelineTarget.NOTIFICATIONS" -> PipelineTarget.NOTIFICATIONS
                        "PipelineTarget.BOTH" -> PipelineTarget.BOTH
                    else -> throw PreprocessingException(
                        "Unknown target specifier $target on" +
                                " pipeline function $functionName. Expected REQUESTS, NOTIFICATIONS or BOTH.")
                    })
                handlerRegistry.extend(resolvedRequestArg.toTypeName(), PipelineHandler(handlerMetadata, pipelineMetadata))
            } else {
                throw PreprocessingException("Pipeline handler $functionName argument next should be of type:" +
                        " suspend (${requestArg.name?.asString()}) -> <HandlerOutputType> and is of type $nextType")
            }
        }
    }
}
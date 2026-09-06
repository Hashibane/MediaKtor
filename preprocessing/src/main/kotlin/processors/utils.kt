package processors

import annotations.HandlerLifespan
import annotations.NotificationParallel
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ksp.toTypeName
import errors.PreprocessingException
import metadata.HandlerDescriptor
import metadata.HandlerMetadata
import metadata.HandlerType
import metadata.NotificationHandler
import metadata.NotificationHandlerMetadata
import metadata.PipelineHandler
import metadata.PipelineMetadata
import metadata.RequestHandler

fun MutableList<HandlerType>.verifyMetadata() {
    val requestHandlerMetadata = this.filterIsInstance<RequestHandler>()
    val inputGroups = requestHandlerMetadata.groupBy { it.handlerMetadata.inputType }
    inputGroups.forEach { name, metadata ->
        if (metadata.size > 1) {
            throw PreprocessingException("The input types must be unique for each request handler. Found request" +
                    "type $name for generated classes ${metadata.first().handlerMetadata.generatedClass.simpleName} and ${
                        metadata.drop(1).first().handlerMetadata.generatedClass.simpleName}")
        }
    }
}

fun MutableList<HandlerType>.addMetadata(function: KSFunctionDeclaration, id: Int, handlerDescriptor: HandlerDescriptor) {
    val functionName = function.simpleName.asString()

    val requestArg = function.parameters.firstOrNull()
        ?: throw PreprocessingException("Handler $functionName must have at least one argument - the request. " +
                "Pass argument of type Unit if no arguments are needed.")

    val resolvedRequestArg = requestArg.type.resolve()
    if (resolvedRequestArg.arguments.isNotEmpty()) {
        throw PreprocessingException("Type $resolvedRequestArg of " +
                "function $functionName cannot be parametrized by other types.")
    }

    val args = function.parameters.drop(1)

    val returnType = function.returnType ?: throw PreprocessingException("Error occured during the resolution" +
            "of return type of handler $functionName.")

    if (handlerDescriptor == HandlerDescriptor.NOTIFICATION_HANDLER) {
        val declaration = returnType.resolve().declaration
        if (declaration.qualifiedName!!.asString() != "kotlin.Unit") {
            throw PreprocessingException("Notification handler should return the Unit type. " +
                    "Caused by function $functionName")
        }
    }

    val packageName = function.packageName.asString()

    val annotationClassName = when (handlerDescriptor) {
        HandlerDescriptor.REQUEST_HANDLER -> annotations.RequestHandler::class.simpleName!!
        HandlerDescriptor.NOTIFICATION_HANDLER -> annotations.NotificationHandler::class.simpleName!!
        HandlerDescriptor.PIPELINE_HANDLER -> annotations.PipelineBehavior::class.simpleName!!
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
            val typeName = it.type.toTypeName()

            ParameterSpec.builder(propName, typeName).build()
        },
        returnType = returnType.toTypeName(),
        origin = function.containingFile,
        lifecycle = when (val lifecycle = functionAnnotationArgs?.find { it.name?.asString() == "lifespan" }?.value.toString()) {
            "HandlerLifespan.SINGLE" -> HandlerLifespan.SINGLE
            "HandlerLifespan.FACTORY" -> HandlerLifespan.FACTORY
            else -> throw PreprocessingException(
                "Unknown lifecycle specifier $lifecycle on" +
                        " function $functionName. Expected single or factory"
            )
        }
    )

    when (handlerDescriptor) {
        HandlerDescriptor.REQUEST_HANDLER -> add(RequestHandler(handlerMetadata))
        HandlerDescriptor.NOTIFICATION_HANDLER -> {
            val parallel = functionAnnotationArgs?.find { it.name?.asString() == "parallel" }?.value.toString()
            val order = functionAnnotationArgs?.find { it.name?.asString() == "order" }?.value.toString()

            val notificationMetadata = NotificationHandlerMetadata(
                when (parallel) {
                    "NotificationParallel.SEQUENTIAL" -> NotificationParallel.SEQUENTIAL
                    "NotificationParallel.PARALLEL" -> NotificationParallel.PARALLEL
                    else -> throw PreprocessingException(
                        "Unknown lifecycle specifier $parallel on" +
                                " function $functionName. Expected sequential or parallel"
                    )
                },
                order.toInt()
            )
            add(NotificationHandler(handlerMetadata, notificationMetadata))
        }
        HandlerDescriptor.PIPELINE_HANDLER -> {
            val order = functionAnnotationArgs?.find { it.name?.asString() == "order" }?.value.toString()
            val pipelineMetadata = PipelineMetadata(order.toInt())
            add(PipelineHandler(handlerMetadata, pipelineMetadata))
        }
    }


}
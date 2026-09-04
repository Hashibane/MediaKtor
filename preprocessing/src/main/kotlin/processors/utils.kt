package processors

import annotations.HandlerLifespan
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ksp.toTypeName
import errors.PreprocessingException
import metadata.HandlerMetadata

fun MutableList<HandlerMetadata>.verifyMetadata() {
    val requestHandlerMetadata = this.filter { !it.isNotificationHandler }
    val inputGroups = requestHandlerMetadata.groupBy { it.inputType }
    inputGroups.forEach { name, metadata ->
        if (metadata.size > 1) {
            throw PreprocessingException("The input types must be unique for each request handler. Found request" +
                    "type $name for generated classes ${metadata.first().generatedClass.simpleName} and ${
                        metadata.drop(1).first().generatedClass.simpleName}")
        }
    }
}

fun MutableList<HandlerMetadata>.addMetadata(function: KSFunctionDeclaration, id: Int, isNotificationHandler: Boolean) {
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

    if (isNotificationHandler) {
        val declaration = returnType.resolve().declaration
        if (declaration.qualifiedName!!.asString() != "kotlin.Unit") {
            throw PreprocessingException("Notification handler should return the Unit type. " +
                    "Caused by function $functionName")
        }
    }

    val packageName = function.packageName.asString()

    val annotationClassName = if (!isNotificationHandler) annotations.RequestHandler::class.simpleName!! else
        annotations.NotificationHandler::class.simpleName!!

    add(
        HandlerMetadata(
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
            lifecycle = when (val lifecycle = function.annotations
                .find { it.shortName.asString() == annotationClassName }
                ?.arguments?.first()?.value.toString()) {
                "HandlerLifespan.SINGLE" -> HandlerLifespan.SINGLE
                "HandlerLifespan.FACTORY" -> HandlerLifespan.FACTORY
                else -> throw PreprocessingException(
                    "Unknown lifecycle specifier $lifecycle on" +
                            " function $functionName. Expected single or factory"
                )
            },
            isNotificationHandler
        )
    )
}
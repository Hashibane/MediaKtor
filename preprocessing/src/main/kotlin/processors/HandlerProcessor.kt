package processors

import annotations.HandlerLifespan
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import errors.PreprocessingException
import generators.di.generateKtorDI
import generators.generateHandler
import generators.generateMediator
import generators.utils.handlerDependencies
import interfaces.RequestHandler
import metadata.HandlerMetadata

class HandlerProcessor(val codeGenerator: CodeGenerator, val logger: KSPLogger) : SymbolProcessor {
    val handlerMetadata: MutableList<HandlerMetadata> = mutableListOf()
    override fun process(resolver: Resolver): List<KSAnnotated> {
        resolver
            .getSymbolsWithAnnotation("annotations.RequestHandler")
            .filter { it.validate() }
            .forEach { it.accept(HandlerVisitor(), Unit) }

        /*
         *  TODO : Metadata verification for:
         *  - Multiple instances of handlers with identical request types
         *  - Type arguments in request types
         */

        handlerMetadata.forEach {
            val handlerSpec = generateHandler(it)
            val dependencies = handlerDependencies(it)
            handlerSpec.writeTo(codeGenerator, dependencies)
        }

        val mediatorMetadata = codeGenerator.generateMediator(handlerMetadata)
        val dependencies = handlerDependencies(handlerMetadata)
        mediatorMetadata?.fileSpec?.writeTo(codeGenerator, dependencies)

        if (mediatorMetadata != null) {
            val diSpec = codeGenerator.generateKtorDI(handlerMetadata, mediatorMetadata.className)
            diSpec.writeTo(codeGenerator, dependencies)
        }

        // Very important not to loop
        handlerMetadata.clear()

        return emptyList()
    }

    private var _id = 0
    val id: Int
        get() {
            _id += 1
            return _id
        }

    inner class HandlerVisitor : KSVisitorVoid() {
        override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: Unit) {
            super.visitFunctionDeclaration(function, data)

            val functionName = function.simpleName.asString()

            val requestArg = function.parameters.firstOrNull()
                ?: throw PreprocessingException("Handler $functionName must have at least one argument - the request. " +
                        "Pass argument of type Unit if no arguments are needed.")

            val args = function.parameters.drop(1)

            val returnType = function.returnType ?: throw PreprocessingException("Error occured during the resolution" +
                    "of return type of handler $functionName.")

            val packageName = function.packageName.asString()

            handlerMetadata.add(
                HandlerMetadata(
                    MemberName(packageName, functionName),
                    generatedClass = ClassName(packageName, "Handler__${functionName}__$id"),
                    inputType = requestArg.type.toTypeName(),
                    args = args.map {
                        val propName = it.name?.asString() ?: "_"
                        val typeName = it.type.toTypeName()

                        ParameterSpec.builder(propName, typeName).build()
                    },
                    returnType = returnType.toTypeName(),
                    origin = function.containingFile,
                    lifecycle = when (val lifecycle = function.annotations
                        .find { it.shortName.asString() == RequestHandler::class.simpleName!! }
                        ?.arguments?.first()?.value.toString()) {
                        "HandlerLifespan.SINGLE" -> HandlerLifespan.SINGLE
                        "HandlerLifespan.FACTORY" -> HandlerLifespan.FACTORY
                        else -> throw PreprocessingException("Unknown lifecycle specifier $lifecycle on" +
                                " function $functionName. Expected single or factory")
                    }
                )
            )
        }
    }
}
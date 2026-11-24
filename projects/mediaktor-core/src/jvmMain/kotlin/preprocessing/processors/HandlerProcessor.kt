package preprocessing.processors

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.ksp.writeTo
import preprocessing.generators.generateHandler
import preprocessing.generators.generateMediator
import preprocessing.generators.handlerDependencies
import preprocessing.metadata.HandlerDescriptor
import preprocessing.metadata.HandlerType

abstract class HandlerProcessor(open val codeGenerator: CodeGenerator, open val logger: KSPLogger) : SymbolProcessor {
    val handlerMetadata: MutableMap<TypeName, MutableList<HandlerType>> = mutableMapOf()
    val inputTypeMetadata: MutableMap<TypeName, MutableList<TypeName>> = mutableMapOf()

    // o1 <: o2 <=> compare > 0
    inner class TypeSorter : Comparator<TypeName>  {
        override fun compare(o1: TypeName?, o2: TypeName?): Int =
            if (inputTypeMetadata[o2]?.contains(o1) == true) {
                return 1
            }
            else if (o2 == null) {
                return 0
            } else {
                return -1
            }
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        resolver
            .getSymbolsWithAnnotation("mediaktor.core.annotations.RequestHandler")
            .filter { it.validate(enableNewFeatures = false) }
            .forEach { it.accept(RequestHandlerVisitor(), Unit) }

        resolver
            .getSymbolsWithAnnotation("mediaktor.core.annotations.NotificationHandler")
            .filter { it.validate(enableNewFeatures = false) }
            .forEach { it.accept(NotificationHandlerVisitor(), Unit) }

        resolver
            .getSymbolsWithAnnotation("mediaktor.core.annotations.PipelineBehavior")
            .filter { it.validate(enableNewFeatures = false) }
            .forEach { it.accept(PipelineHandlerVisitor(), Unit) }

        handlerMetadata.verifyMetadata()

        handlerMetadata.forEach { (_, handlers) ->
            handlers.forEach {
                val handlerSpec = generateHandler(it)
                val dependencies = handlerDependencies(it)
                handlerSpec.writeTo(codeGenerator, dependencies)
            }
        }

        val allHandlers = handlerMetadata.flatMap { it.value }
        val mediatorMetadata = generateMediator(handlerMetadata, TypeSorter(), logger)
        val dependencies = handlerDependencies(allHandlers)
        mediatorMetadata?.fileSpec?.writeTo(codeGenerator, dependencies)

        if (mediatorMetadata != null) {
            val diSpec = codeGenerator.generateDI(allHandlers, mediatorMetadata.className)
            diSpec.writeTo(codeGenerator, dependencies)
        }

        // Very important not to loop
        handlerMetadata.clear()

        return emptyList()
    }

    abstract fun CodeGenerator.generateDI(handlers: List<HandlerType>, mediatorClass: ClassName): FileSpec

    inner class RequestHandlerVisitor : KSVisitorVoid() {
        override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: Unit) {
            super.visitFunctionDeclaration(function, data)

            addMetadata(handlerMetadata, function, HandlerDescriptor.REQUEST_HANDLER, inputTypeMetadata)
        }
    }

    inner class NotificationHandlerVisitor : KSVisitorVoid() {
        override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: Unit) {
            super.visitFunctionDeclaration(function, data)

            addMetadata(handlerMetadata, function,  HandlerDescriptor.NOTIFICATION_HANDLER, inputTypeMetadata)
        }
    }

    inner class PipelineHandlerVisitor : KSVisitorVoid() {
        override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: Unit) {
            super.visitFunctionDeclaration(function, data)

            addMetadata(handlerMetadata, function, HandlerDescriptor.PIPELINE_HANDLER, inputTypeMetadata)
        }
    }
}
package processors

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.ksp.writeTo
import errors.PreprocessingException
import generators.di.generateKtorDI
import generators.generateHandler
import generators.generateMediator
import generators.utils.handlerDependencies
import metadata.HandlerDescriptor
import metadata.HandlerMetadata
import metadata.HandlerType

class HandlerProcessor(val codeGenerator: CodeGenerator, val logger: KSPLogger) : SymbolProcessor {
    val handlerMetadata: MutableList<HandlerType> = mutableListOf()
    override fun process(resolver: Resolver): List<KSAnnotated> {
        resolver
            .getSymbolsWithAnnotation("annotations.RequestHandler")
            .filter { it.validate() }
            .forEach { it.accept(RequestHandlerVisitor(), Unit) }

        resolver
            .getSymbolsWithAnnotation("annotations.NotificationHandler")
            .filter { it.validate() }
            .forEach { it.accept(NotificationHandlerVisitor(), Unit) }

        handlerMetadata.verifyMetadata()

        handlerMetadata.forEach {
            val handlerSpec = generateHandler(it)
            val dependencies = handlerDependencies(it)
            handlerSpec.writeTo(codeGenerator, dependencies)
        }

        val mediatorMetadata = generateMediator(handlerMetadata)
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

    inner class RequestHandlerVisitor : KSVisitorVoid() {
        override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: Unit) {
            super.visitFunctionDeclaration(function, data)

            handlerMetadata.addMetadata(function, id, HandlerDescriptor.REQUEST_HANDLER)
        }
    }

    inner class NotificationHandlerVisitor : KSVisitorVoid() {
        override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: Unit) {
            super.visitFunctionDeclaration(function, data)

            handlerMetadata.addMetadata(function, id, HandlerDescriptor.NOTIFICATION_HANDLER)
        }
    }
}
package processors

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ksp.toTypeName
import errors.PreprocessingException
import generators.HandlerGenerator.generateHandler
import metadata.HandlerMetadata

class HandlerProcessor(val codeGenerator: CodeGenerator, val logger: KSPLogger) : SymbolProcessor {
    val handlerMetadata: MutableList<HandlerMetadata> = mutableListOf()
    override fun process(resolver: Resolver): List<KSAnnotated> {
        resolver
            .getSymbolsWithAnnotation("annotations.RequestHandler")
            .filter { it.validate() }
            .forEach { it.accept(HandlerVisitor(), Unit) }


        handlerMetadata.forEach { codeGenerator.generateHandler(it) }

        // Very important not to loop
        handlerMetadata.clear()

        return emptyList()
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

            handlerMetadata.add(
                HandlerMetadata(
                    MemberName(function.packageName.asString(), functionName),
                    inputType = requestArg.type.toTypeName(),
                    args = args.map {
                        val propName = it.name?.asString() ?: "_"
                        val typeName = it.type.toTypeName()

                        ParameterSpec.builder(propName, typeName).build()
                    },
                    returnType = returnType.toTypeName(),
                    origin = function.containingFile,
                )
            )

        }
    }
}
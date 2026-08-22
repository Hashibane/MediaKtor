package processors

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.devtools.ksp.validate
import errors.PreprocessingException
import generators.generateHandler

class HandlerProcessor(val codeGenerator: CodeGenerator) : SymbolProcessor {
    private var _uniqueId = 0
    private val uniqueId: Int
        get() {
            _uniqueId += 1
            return _uniqueId
        }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        resolver
            .getSymbolsWithAnnotation("annotations.RequestHandler")
            .filter { it.validate() }
            .forEach { it.accept(HandlerVisitor(), Unit) }

        return emptyList()
    }

    inner class HandlerVisitor : KSVisitorVoid() {

        override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: Unit) {
            super.visitFunctionDeclaration(function, data)

            val functionName = function.qualifiedName ?: function.simpleName

            val requestArg = function.parameters.firstOrNull()
                ?: throw PreprocessingException("Handler $functionName must have at least one argument - the request. " +
                        "Pass argument of type Unit if no arguments are needed.")

            val args = function.parameters.drop(1)

            val returnType = function.returnType ?: throw PreprocessingException("Error occured during the resolution" +
                    "of return type of handler $functionName.")

            generateHandler(codeGenerator, function, requestArg, args, returnType, uniqueId)
        }
    }
}
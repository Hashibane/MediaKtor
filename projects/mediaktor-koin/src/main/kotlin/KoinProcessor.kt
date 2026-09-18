import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import preprocessing.metadata.HandlerType
import preprocessing.processors.HandlerProcessor

class KoinProcessor(override val codeGenerator: CodeGenerator, override val logger: KSPLogger) : HandlerProcessor(codeGenerator, logger) {
    override fun CodeGenerator.generateDI(
        handlers: List<HandlerType>,
        mediatorClass: ClassName
    ): FileSpec = generateKoin(handlers, mediatorClass)
}
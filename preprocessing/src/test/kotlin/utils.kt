import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import io.mockk.every
import io.mockk.mockkClass
import processors.HandlerProcessor
import java.io.ByteArrayOutputStream

fun generateStringOutput(nHandlers: Int, body: HandlerProcessor.() -> Unit): List<String> {
    val outputStreams = mutableListOf<ByteArrayOutputStream>()
    // one per handler + 1 mediator + one DI
    repeat(nHandlers + 2) {
        outputStreams.add(ByteArrayOutputStream())
    }

    val codeGenerator = mockkClass(CodeGenerator::class)
    every { codeGenerator.createNewFile(any(), any(), any()) } returnsMany outputStreams

    val logger = mockkClass(KSPLogger::class)
    val processor = HandlerProcessor(codeGenerator, logger)

    processor.body()

    return outputStreams.map { it.toString() }
}
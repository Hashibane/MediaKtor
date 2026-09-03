import annotations.HandlerLifespan
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassifierReference
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.Nullability
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


internal fun KSFunctionDeclaration.setupHandler(handlerName: String, lifespan: HandlerLifespan = HandlerLifespan.SINGLE) {
    packageName { "handlers" }
    containingFile {
        packageName { "handlers" }
    }

    simpleName { handlerName }

    annotation {
        shortName { "RequestHandler" }
        annotationType {
            type {
                declaration {
                    packageName { "annotations" }
                    qualifiedName { "annotations.RequestHandler" }
                }
            }
        }

        argument {
            value {
                classDeclaration {
                    packageName { "annotations" }
                    qualifiedName { "annotations.HandlerLifespan.$lifespan" }
                    classKind { ClassKind.ENUM_ENTRY }

                    every { this@classDeclaration.toString() } returns "HandlerLifespan.$lifespan"
                    parentClassDeclaration {
                        packageName { "annotations" }
                        qualifiedName { "annotations.HandlerLifespan" }
                        classKind { ClassKind.ENUM_CLASS }
                    }
                }
            }
        }
    }
}

internal fun KSFunctionDeclaration.setupHandlerReturn(handlerName: String, lifespan: HandlerLifespan = HandlerLifespan.SINGLE) {
    setupHandler(handlerName, lifespan)

    returnType {
        type {
            nullability { Nullability.NULLABLE }
            classDeclaration {
                packageName { "returnPackage" }
                qualifiedName { "returnPackage.test" }
                classKind { ClassKind.CLASS }
            }
        }
    }
}

internal fun KSFunctionDeclaration.setupHandlerLifespan(handlerName: String, lifespan: HandlerLifespan) {
    setupHandlerReturn(handlerName, lifespan)

    parameter {
        name { "first" }
        typeRef {
            type {
                nullability { Nullability.NULLABLE }
                classDeclaration {
                    packageName { "paramPackage" }
                    qualifiedName { "paramPackage.test" }
                    classKind { ClassKind.CLASS }
                }
            }
            element {
                mockkClass(KSClassifierReference::class)
            }
        }
    }
}
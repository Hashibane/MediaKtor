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


internal fun KSFunctionDeclaration.setupHandler(handlerName: String, lifespan: HandlerLifespan = HandlerLifespan.SINGLE,
                                                isNotificationHandler: Boolean = false) {
    packageName { "handlers" }
    containingFile {
        packageName { "handlers" }
    }

    simpleName { handlerName }

    val name = if (isNotificationHandler) "NotificationHandler" else "RequestHandler"
    annotation {
        shortName { name }
        annotationType {
            type {
                declaration {
                    packageName { "annotations" }
                    qualifiedName { "annotations.$name" }
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

internal fun KSFunctionDeclaration.setupHandlerReturn(handlerName: String,
                                                      lifespan: HandlerLifespan = HandlerLifespan.SINGLE, isNotificationHandler: Boolean = false) {
    setupHandler(handlerName, lifespan, isNotificationHandler)

    returnType {
        type {
            nullability { Nullability.NULLABLE }
            classDeclaration {
                packageName { "kotlin" }
                qualifiedName { "kotlin.Unit" }
                classKind { ClassKind.CLASS }
            }
        }
    }
}

internal fun KSFunctionDeclaration.setupHandlerLifespan(handlerName: String, lifespan: HandlerLifespan, isNotificationHandler: Boolean = false) {
    setupHandlerReturn(handlerName, lifespan, isNotificationHandler)

    parameter {
        name { "first" }
        typeRef {
            type {
                nullability { Nullability.NULLABLE }
                classDeclaration {
                    packageName { "paramPackage" }
                    qualifiedName { "paramPackage.type__$handlerName" }
                    classKind { ClassKind.CLASS }
                }
            }
            element {
                mockkClass(KSClassifierReference::class)
            }
        }
    }
}
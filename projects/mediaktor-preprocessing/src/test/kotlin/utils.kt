import annotations.HandlerLifespan
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassifierReference
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.Nullability
import io.mockk.every
import io.mockk.mockkClass
import metadata.AdditionalData
import metadata.NotificationHandlerMetadata
import metadata.PipelineHandler
import metadata.PipelineMetadata
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

    val logger = MockKSPLogger
    val processor = HandlerProcessor(codeGenerator, logger)

    processor.body()

    return outputStreams.map { it.toString() }
}


internal fun KSFunctionDeclaration.setupHandler(handlerName: String,
                                                lifespan: HandlerLifespan = HandlerLifespan.SINGLE,
                                                additionalData: AdditionalData? = null) {
    packageName { "handlers" }
    containingFile {
        packageName { "handlers" }
    }

    simpleName { handlerName }

    val name = when (additionalData) {
        null -> "RequestHandler"
        is NotificationHandlerMetadata -> "NotificationHandler"
        is PipelineMetadata -> "PipelineBehavior"
    }
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
            name { "lifespan" }

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
        if (additionalData is NotificationHandlerMetadata) {
            argument {
                name { "parallel" }

                value {
                    classDeclaration {
                        packageName { "annotations" }
                        qualifiedName { "annotations.NotificationParallel.${additionalData.parallel}" }
                        classKind { ClassKind.ENUM_ENTRY }

                        every { this@classDeclaration.toString() } returns "NotificationParallel.${additionalData.parallel}"
                        parentClassDeclaration {
                            packageName { "annotations" }
                            qualifiedName { "annotations.NotificationParallel" }
                            classKind { ClassKind.ENUM_CLASS }
                        }
                    }
                }
            }

            argument {
                name { "order" }

                value {
                    additionalData.order
                }
            }
        }

        if (additionalData is PipelineMetadata) {
            argument {
                name { "target" }

                value {
                    classDeclaration {
                        packageName { "annotations" }
                        qualifiedName { "annotations.PipelineTarget.${additionalData.target}" }
                        classKind { ClassKind.ENUM_ENTRY }

                        every { this@classDeclaration.toString() } returns "PipelineTarget.${additionalData.target}"
                        parentClassDeclaration {
                            packageName { "annotations" }
                            qualifiedName { "annotations.PipelineTarget" }
                            classKind { ClassKind.ENUM_CLASS }
                        }
                    }
                }
            }


            argument {
                name { "order" }

                value {
                    additionalData.order
                }
            }
        }
    }
}

internal fun KSFunctionDeclaration.setupHandlerReturn(handlerName: String,
                                                      lifespan: HandlerLifespan = HandlerLifespan.SINGLE,
                                                      notificationHandlerData: NotificationHandlerMetadata? = null) {
    setupHandler(handlerName, lifespan, notificationHandlerData)

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

internal fun KSFunctionDeclaration.setupHandlerLifespan(handlerName: String, lifespan: HandlerLifespan,
                                                        notificationHandlerData: NotificationHandlerMetadata? = null) {
    setupHandlerReturn(handlerName, lifespan, notificationHandlerData)

    parameter {
        name { "first" }
        typeRef {
            type {
                nullability { Nullability.NOT_NULL }
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
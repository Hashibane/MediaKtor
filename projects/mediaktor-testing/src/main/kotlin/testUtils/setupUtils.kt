package testUtils

import mediaktor.core.annotations.HandlerLifespan
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassifierReference
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.Nullability
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import io.mockk.every
import io.mockk.mockkClass
import preprocessing.metadata.AdditionalData
import preprocessing.metadata.HandlerType
import preprocessing.metadata.NotificationHandlerMetadata
import preprocessing.metadata.PipelineMetadata
import preprocessing.processors.HandlerProcessor
import java.io.ByteArrayOutputStream

fun generateStringOutput(nHandlers: Int,
                         handlerProcessorProvider: (CodeGenerator, KSPLogger) -> HandlerProcessor? = { _, _ -> null },
                         body: HandlerProcessor.() -> Unit): List<String> {
    val outputStreams = mutableListOf<ByteArrayOutputStream>()
    // one per handler + 1 mediator + one DI
    repeat(nHandlers + 2) {
        outputStreams.add(ByteArrayOutputStream())
    }

    val codeGenerator = mockkClass(CodeGenerator::class)
    every { codeGenerator.createNewFile(any(), any(), any()) } returnsMany outputStreams

    val logger = MockKSPLogger
    val processor = handlerProcessorProvider(codeGenerator, logger) ?: object : HandlerProcessor(codeGenerator, logger) {
        override fun CodeGenerator.generateDI(
            handlers: List<HandlerType>,
            mediatorClass: ClassName
        ): FileSpec {
            return FileSpec.builder("", "").build()
        }

    }

    processor.body()

    return outputStreams.map { it.toString() }
}


fun KSFunctionDeclaration.setupHandler(handlerName: String,
                                       packageName: String,
                                                lifespan: HandlerLifespan = HandlerLifespan.SINGLE,
                                                additionalData: AdditionalData? = null) {
    packageName { packageName }
    containingFile {
        packageName { packageName }
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
                    packageName { "mediaktor.core.annotations" }
                    qualifiedName { "mediaktor.core.annotations.$name" }
                }
            }
        }

        argument {
            name { "lifespan" }

            value {
                classDeclaration {
                    packageName { "mediaktor.core.annotations" }
                    qualifiedName { "mediaktor.core.annotations.HandlerLifespan.$lifespan" }
                    classKind { ClassKind.ENUM_ENTRY }

                    every { this@classDeclaration.toString() } returns "HandlerLifespan.$lifespan"
                    parentClassDeclaration {
                        packageName { "mediaktor.core.annotations" }
                        qualifiedName { "mediaktor.core.annotations.HandlerLifespan" }
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
                        packageName { "mediaktor.core.annotations" }
                        qualifiedName { "mediaktor.core.annotations.NotificationParallel.${additionalData.parallel}" }
                        classKind { ClassKind.ENUM_ENTRY }

                        every { this@classDeclaration.toString() } returns "NotificationParallel.${additionalData.parallel}"
                        parentClassDeclaration {
                            packageName { "mediaktor.core.annotations" }
                            qualifiedName { "mediaktor.core.annotations.NotificationParallel" }
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
                        packageName { "mediaktor.core.annotations" }
                        qualifiedName { "mediaktor.core.annotations.PipelineTarget.${additionalData.target}" }
                        classKind { ClassKind.ENUM_ENTRY }

                        every { this@classDeclaration.toString() } returns "PipelineTarget.${additionalData.target}"
                        parentClassDeclaration {
                            packageName { "mediaktor.core.annotations" }
                            qualifiedName { "mediaktor.core.annotations.PipelineTarget" }
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

fun KSFunctionDeclaration.setupHandlerReturn(handlerName: String,
                                             packageName: String,
                                                      lifespan: HandlerLifespan = HandlerLifespan.SINGLE,
                                                      notificationHandlerData: NotificationHandlerMetadata? = null,
                                             ) {
    setupHandler(handlerName, packageName, lifespan, notificationHandlerData)

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

fun KSFunctionDeclaration.setupHandlerLifespan(handlerName: String, packageName: String,
                                                        lifespan: HandlerLifespan,
                                                        notificationHandlerData: NotificationHandlerMetadata? = null,
                                               ) {
    setupHandlerReturn(handlerName, packageName, lifespan, notificationHandlerData)

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
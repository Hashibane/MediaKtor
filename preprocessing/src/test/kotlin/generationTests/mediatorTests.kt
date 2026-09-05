package generationTests

import annotations.NotificationParallel
import classDeclaration
import classKind
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassifierReference
import com.google.devtools.ksp.symbol.Nullability
import element
import functionDeclaration
import generateStringOutput
import io.mockk.every
import io.mockk.mockkClass
import metadata.NotificationHandlerMetadata
import nullability
import packageName
import parameter
import processors.HandlerProcessor
import qualifiedName
import returnType
import setupHandler
import setupHandlerReturn
import type
import typeRef
import kotlin.test.Test

class MediatorTests {
    @Test
    fun `multiple handlers test`() {
        val inputOneClass = "TestInputClass1"
        val outputOneClass = "TestReturnClass1"
        val handlerOneName = "testHandler1"

        val handlerOne = functionDeclaration {
            setupHandler(handlerOneName)

            parameter {
                typeRef {
                    type {
                        nullability { Nullability.NOT_NULL }
                        classDeclaration {
                            packageName { "paramPackage" }
                            qualifiedName { "paramPackage.$inputOneClass" }
                            classKind { ClassKind.CLASS }
                        }
                    }
                    element {
                        mockkClass(KSClassifierReference::class)
                    }
                }
            }

            returnType {
                type {
                    nullability { Nullability.NOT_NULL }
                    classDeclaration {
                        packageName { "returnPackage" }
                        qualifiedName { "returnPackage.$outputOneClass" }
                        classKind { ClassKind.CLASS }
                    }
                }
            }
        }

        val inputTwoClass = "TestInputClass2"
        val outputTwoClass = "TestReturnClass2"
        val handlerTwoName = "testHandler2"

        val handlerTwo = functionDeclaration {
            setupHandler(handlerTwoName)

            parameter {
                typeRef {
                    type {
                        nullability { Nullability.NOT_NULL }
                        classDeclaration {
                            packageName { "paramPackage" }
                            qualifiedName { "paramPackage.$inputTwoClass" }
                            classKind { ClassKind.CLASS }
                        }
                    }
                    element {
                        mockkClass(KSClassifierReference::class)
                    }
                }
            }

            returnType {
                type {
                    nullability { Nullability.NOT_NULL }
                    classDeclaration {
                        packageName { "returnPackage" }
                        qualifiedName { "returnPackage.$outputTwoClass" }
                        classKind { ClassKind.CLASS }
                    }
                }
            }
        }

        val generatedCode = generateStringOutput(1) {
            val resolver = mockkClass(Resolver::class)
            every { resolver.getSymbolsWithAnnotation("annotations.RequestHandler") } returns sequenceOf()
            every { resolver.getSymbolsWithAnnotation("annotations.NotificationHandler") } returns sequenceOf()

            RequestHandlerVisitor().visitFunctionDeclaration(handlerOne, Unit)
            RequestHandlerVisitor().visitFunctionDeclaration(handlerTwo, Unit)
            process(resolver)
        }

        val mediatorCode = generatedCode.drop(2).first()
        assert(mediatorCode.contains(": Mediator"))
        assert(mediatorCode.contains("override suspend operator fun <T : Any> invoke"))
        assert(mediatorCode.contains(": suspend () -> Handler__${handlerOneName}__1"))
        assert(mediatorCode.contains(": suspend () -> Handler__${handlerTwoName}__2"))
        assert(mediatorCode.contains("is $inputOneClass ->"))
        assert(mediatorCode.contains("is $inputTwoClass ->"))
    }

    @Test
    fun `multiple handlers with nullable request type`() {
        val inputOneClass = "TestInputClass1"
        val handlerOneName = "testHandler1"

        val handlerOne = functionDeclaration {
            setupHandlerReturn(handlerOneName)

            parameter {
                typeRef {
                    type {
                        nullability { Nullability.NULLABLE }
                        classDeclaration {
                            packageName { "paramPackage" }
                            qualifiedName { "paramPackage.$inputOneClass" }
                            classKind { ClassKind.CLASS }
                        }
                    }
                    element {
                        mockkClass(KSClassifierReference::class)
                    }
                }
            }
        }

        val inputTwoClass = "TestInputClass2"
        val handlerTwoName = "testHandler2"

        val handlerTwo = functionDeclaration {
            setupHandlerReturn(handlerTwoName)

            parameter {
                typeRef {
                    type {
                        nullability { Nullability.NULLABLE }
                        classDeclaration {
                            packageName { "paramPackage" }
                            qualifiedName { "paramPackage.$inputTwoClass" }
                            classKind { ClassKind.CLASS }
                        }
                    }
                    element {
                        mockkClass(KSClassifierReference::class)
                    }
                }
            }
        }

        val generatedCode = generateStringOutput(1) {
            val resolver = mockkClass(Resolver::class)
            every { resolver.getSymbolsWithAnnotation("annotations.RequestHandler") } returns sequenceOf()
            every { resolver.getSymbolsWithAnnotation("annotations.NotificationHandler") } returns sequenceOf()

            RequestHandlerVisitor().visitFunctionDeclaration(handlerOne, Unit)
            RequestHandlerVisitor().visitFunctionDeclaration(handlerTwo, Unit)
            process(resolver)
        }

        val mediatorCode = generatedCode.drop(2).first()
        assert(mediatorCode.contains(": Mediator"))
        assert(mediatorCode.contains("override suspend operator fun <T : Any> invoke"))
        assert(mediatorCode.contains(": suspend () -> Handler__${handlerOneName}__1"))
        assert(mediatorCode.contains(": suspend () -> Handler__${handlerTwoName}__2"))
        assert(mediatorCode.contains("is $inputOneClass? ->"))
        assert(mediatorCode.contains("is $inputTwoClass? ->"))
    }

    @Test
    fun `multiple notifiers with same input type test`() {
        val inputOneClass = "TestInputClass1"
        val handlerOneName = "testHandler1"

        val handlerOne = functionDeclaration {
            setupHandlerReturn(handlerOneName,
                notificationHandlerData = NotificationHandlerMetadata(NotificationParallel.SEQUENTIAL, 1))

            parameter {
                typeRef {
                    type {
                        nullability { Nullability.NOT_NULL }
                        classDeclaration {
                            packageName { "paramPackage" }
                            qualifiedName { "paramPackage.$inputOneClass" }
                            classKind { ClassKind.CLASS }
                        }
                    }
                    element {
                        mockkClass(KSClassifierReference::class)
                    }
                }
            }
        }

        val handlerTwoName = "testHandler2"

        val handlerTwo = functionDeclaration {
            setupHandlerReturn(handlerTwoName,
                notificationHandlerData = NotificationHandlerMetadata(NotificationParallel.SEQUENTIAL, 1))

            parameter {
                typeRef {
                    type {
                        nullability { Nullability.NOT_NULL }
                        classDeclaration {
                            packageName { "paramPackage" }
                            qualifiedName { "paramPackage.$inputOneClass" }
                            classKind { ClassKind.CLASS }
                        }
                    }
                    element {
                        mockkClass(KSClassifierReference::class)
                    }
                }
            }
        }

        val generatedCode = generateStringOutput(1) {
            val resolver = mockkClass(Resolver::class)
            every { resolver.getSymbolsWithAnnotation("annotations.RequestHandler") } returns sequenceOf()
            every { resolver.getSymbolsWithAnnotation("annotations.NotificationHandler") } returns sequenceOf()

            NotificationHandlerVisitor().visitFunctionDeclaration(handlerOne, Unit)
            NotificationHandlerVisitor().visitFunctionDeclaration(handlerTwo, Unit)
            process(resolver)
        }

        val mediatorCode = generatedCode.drop(2).first()
        assert(mediatorCode.contains(": Mediator"))
        assert(mediatorCode.contains("override suspend fun <T : Any> publish"))
        assert(mediatorCode.contains(": suspend () -> Handler__${handlerOneName}__1"))
        assert(mediatorCode.contains(": suspend () -> Handler__${handlerTwoName}__2"))
        assert(mediatorCode.contains("is $inputOneClass ->"))
        assert(mediatorCode.contains("handler__${handlerOneName.lowercase()}__1().handleRequest(command)"))
        assert(mediatorCode.contains("handler__${handlerTwoName.lowercase()}__2().handleRequest(command)"))
    }

    @Test
    fun `multiple notifiers with same different type test`() {
        val inputOneClass = "TestInputClass1"
        val handlerOneName = "testHandler1"

        val handlerOne = functionDeclaration {
            setupHandlerReturn(handlerOneName,
                notificationHandlerData = NotificationHandlerMetadata(NotificationParallel.SEQUENTIAL, 1))

            parameter {
                typeRef {
                    type {
                        nullability { Nullability.NOT_NULL }
                        classDeclaration {
                            packageName { "paramPackage" }
                            qualifiedName { "paramPackage.$inputOneClass" }
                            classKind { ClassKind.CLASS }
                        }
                    }
                    element {
                        mockkClass(KSClassifierReference::class)
                    }
                }
            }
        }

        val handlerTwoName = "testHandler2"
        val inputTwoClass = "TestInputClass2"

        val handlerTwo = functionDeclaration {
            setupHandlerReturn(handlerTwoName,
                notificationHandlerData = NotificationHandlerMetadata(NotificationParallel.SEQUENTIAL, 1))

            parameter {
                typeRef {
                    type {
                        nullability { Nullability.NOT_NULL }
                        classDeclaration {
                            packageName { "paramPackage" }
                            qualifiedName { "paramPackage.$inputTwoClass" }
                            classKind { ClassKind.CLASS }
                        }
                    }
                    element {
                        mockkClass(KSClassifierReference::class)
                    }
                }
            }
        }

        val generatedCode = generateStringOutput(1) {
            val resolver = mockkClass(Resolver::class)
            every { resolver.getSymbolsWithAnnotation("annotations.RequestHandler") } returns sequenceOf()
            every { resolver.getSymbolsWithAnnotation("annotations.NotificationHandler") } returns sequenceOf()

            NotificationHandlerVisitor().visitFunctionDeclaration(handlerOne, Unit)
            NotificationHandlerVisitor().visitFunctionDeclaration(handlerTwo, Unit)
            process(resolver)
        }

        val mediatorCode = generatedCode.drop(2).first()
        assert(mediatorCode.contains(": Mediator"))
        assert(mediatorCode.contains("override suspend fun <T : Any> publish"))
        assert(mediatorCode.contains(": suspend () -> Handler__${handlerOneName}__1"))
        assert(mediatorCode.contains(": suspend () -> Handler__${handlerTwoName}__2"))
        assert(mediatorCode.contains("is $inputOneClass ->"))
        assert(mediatorCode.contains("is $inputTwoClass ->"))
        assert(mediatorCode.contains("handler__${handlerOneName.lowercase()}__1().handleRequest(command)"))
        assert(mediatorCode.contains("handler__${handlerTwoName.lowercase()}__2().handleRequest(command)"))
    }
}
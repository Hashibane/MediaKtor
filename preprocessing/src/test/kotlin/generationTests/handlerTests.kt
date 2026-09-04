package generationTests
import argument
import classDeclaration
import classKind
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassifierReference
import com.google.devtools.ksp.symbol.Nullability
import com.google.devtools.ksp.symbol.Variance
import element
import functionDeclaration
import generateStringOutput
import io.mockk.every
import io.mockk.mockkClass
import name
import nullability
import packageName
import parameter
import qualifiedName
import returnType
import setupHandler
import type
import typeRef
import variance
import kotlin.test.Test

class HandlerTests {
    @Test
    fun `request handler type without modifications`() {
        val inputClass = "TestInputClass"
        val outputClass = "TestReturnClass"
        val handlerName = "testHandler"

        val funDecl = functionDeclaration {
            setupHandler(handlerName)

            parameter {
                typeRef {
                    type {
                        nullability { Nullability.NOT_NULL }
                        classDeclaration {
                            packageName { "paramPackage" }
                            qualifiedName { "paramPackage.$inputClass" }
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
                        qualifiedName { "returnPackage.$outputClass" }
                        classKind { ClassKind.CLASS }
                    }
                }
            }
        }

        val generatedCode = generateStringOutput(1) {
            val resolver = mockkClass(Resolver::class)
            every { resolver.getSymbolsWithAnnotation("annotations.RequestHandler") } returns sequenceOf()
            every { resolver.getSymbolsWithAnnotation("annotations.NotificationHandler") } returns sequenceOf()

            RequestHandlerVisitor().visitFunctionDeclaration(funDecl, Unit)
            process(resolver)
        }

        val handlerCode = generatedCode.first()
        assert(handlerCode.contains("class Handler__${handlerName}__1"))
        assert(handlerCode.contains(": RequestHandler<$inputClass, $outputClass>"))
        assert(handlerCode.contains("= $handlerName"))
        assert(handlerCode.contains("override suspend fun handleRequest(request: $inputClass): $outputClass"))
    }

    @Test
    fun `request handler nullable type`() {
        val inputClass = "TestInputClass"
        val outputClass = "TestReturnClass"
        val handlerName = "testHandler"

        val funDecl = functionDeclaration {
            setupHandler(handlerName)

            parameter {
                typeRef {
                    type {
                        nullability { Nullability.NULLABLE }
                        classDeclaration {
                            packageName { "paramPackage" }
                            qualifiedName { "paramPackage.$inputClass" }
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
                    nullability { Nullability.NULLABLE }
                    classDeclaration {
                        packageName { "returnPackage" }
                        qualifiedName { "returnPackage.$outputClass" }
                        classKind { ClassKind.CLASS }
                    }
                }
            }
        }

        val generatedCode = generateStringOutput(1) {
            val resolver = mockkClass(Resolver::class)
            every { resolver.getSymbolsWithAnnotation("annotations.RequestHandler") } returns sequenceOf()
            every { resolver.getSymbolsWithAnnotation("annotations.NotificationHandler") } returns sequenceOf()

            RequestHandlerVisitor().visitFunctionDeclaration(funDecl, Unit)
            process(resolver)
        }

        val handlerCode = generatedCode.first()
        assert(handlerCode.contains("class Handler__${handlerName}__1"))
        assert(handlerCode.contains(": RequestHandler<$inputClass?, $outputClass?>"))
        assert(handlerCode.contains("= $handlerName"))
        assert(handlerCode.contains("override suspend fun handleRequest(request: $inputClass?): $outputClass?"))
    }

    @Test
    fun `request handler covariant non-nullable-parametrized return type`() {
        val inputClass = "TestInputClass"
        val outputClass = "TestReturnClass"
        val handlerName = "testHandler"
        val typeParamName = "TypeParam"

        val funDecl = functionDeclaration {
            setupHandler(handlerName)

            parameter {
                typeRef {
                    type {
                        nullability { Nullability.NULLABLE }
                        classDeclaration {
                            packageName { "paramPackage" }
                            qualifiedName { "paramPackage.$inputClass" }
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
                    nullability { Nullability.NULLABLE }
                    argument {
                        typeRef {
                            type {
                                nullability { Nullability.NOT_NULL }
                                classDeclaration {
                                    packageName { "paramPackage" }
                                    qualifiedName { "paramPackage.$typeParamName" }
                                    classKind { ClassKind.CLASS }
                                }
                            }
                        }
                        variance { Variance.COVARIANT }
                    }

                    classDeclaration {
                        packageName { "returnPackage" }
                        qualifiedName { "returnPackage.$outputClass" }
                        classKind { ClassKind.CLASS }
                    }
                }
            }
        }

        val generatedCode = generateStringOutput(1) {
            val resolver = mockkClass(Resolver::class)
            every { resolver.getSymbolsWithAnnotation("annotations.RequestHandler") } returns sequenceOf()
            every { resolver.getSymbolsWithAnnotation("annotations.NotificationHandler") } returns sequenceOf()

            RequestHandlerVisitor().visitFunctionDeclaration(funDecl, Unit)
            process(resolver)
        }

        val handlerCode = generatedCode.first()
        assert(handlerCode.contains("class Handler__${handlerName}__1"))
        assert(handlerCode.contains(": RequestHandler<$inputClass?, $outputClass<out $typeParamName>?>"))
        assert(handlerCode.contains("= $handlerName"))
        assert(handlerCode.contains("override suspend fun handleRequest(request: $inputClass?): " +
                "$outputClass<out $typeParamName>?"))

    }

    @Test
    fun `request handler nullable-parametrized return type`() {
        val inputClass = "TestInputClass"
        val outputClass = "TestReturnClass"
        val handlerName = "testHandler"
        val typeParamName = "TypeParam"

        val funDecl = functionDeclaration {
            setupHandler(handlerName)

            parameter {
                typeRef {
                    type {
                        nullability { Nullability.NULLABLE }
                        classDeclaration {
                            packageName { "paramPackage" }
                            qualifiedName { "paramPackage.$inputClass" }
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
                    nullability { Nullability.NULLABLE }
                    argument {
                        typeRef {
                            type {
                                nullability { Nullability.NULLABLE }
                                classDeclaration {
                                    packageName { "paramPackage" }
                                    qualifiedName { "paramPackage.$typeParamName" }
                                    classKind { ClassKind.CLASS }
                                }
                            }
                        }
                        variance { Variance.INVARIANT }
                    }

                    classDeclaration {
                        packageName { "returnPackage" }
                        qualifiedName { "returnPackage.$outputClass" }
                        classKind { ClassKind.CLASS }
                    }
                }
            }
        }

        val generatedCode = generateStringOutput(1) {
            val resolver = mockkClass(Resolver::class)
            every { resolver.getSymbolsWithAnnotation("annotations.RequestHandler") } returns sequenceOf()
            every { resolver.getSymbolsWithAnnotation("annotations.NotificationHandler") } returns sequenceOf()

            RequestHandlerVisitor().visitFunctionDeclaration(funDecl, Unit)
            process(resolver)
        }

        val handlerCode = generatedCode.first()
        assert(handlerCode.contains("class Handler__${handlerName}__1"))
        assert(handlerCode.contains(": RequestHandler<$inputClass?, $outputClass<$typeParamName?>?>"))
        assert(handlerCode.contains("= $handlerName"))
        assert(handlerCode.contains("override suspend fun handleRequest(request: $inputClass?): " +
                "$outputClass<$typeParamName?>?"))

    }

    @Test
    fun `request handler star-parametrized return type`() {
        val inputClass = "TestInputClass"
        val outputClass = "TestReturnClass"
        val handlerName = "testHandler"
        val funDecl = functionDeclaration {
            setupHandler(handlerName)

            parameter {
                typeRef {
                    type {
                        nullability { Nullability.NULLABLE }
                        classDeclaration {
                            packageName { "paramPackage" }
                            qualifiedName { "paramPackage.$inputClass" }
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
                    nullability { Nullability.NULLABLE }
                    argument {
                        variance { Variance.STAR }
                    }

                    classDeclaration {
                        packageName { "returnPackage" }
                        qualifiedName { "returnPackage.$outputClass" }
                        classKind { ClassKind.CLASS }
                    }
                }
            }
        }

        val generatedCode = generateStringOutput(1) {
            val resolver = mockkClass(Resolver::class)
            every { resolver.getSymbolsWithAnnotation("annotations.RequestHandler") } returns sequenceOf()
            every { resolver.getSymbolsWithAnnotation("annotations.NotificationHandler") } returns sequenceOf()

            RequestHandlerVisitor().visitFunctionDeclaration(funDecl, Unit)
            process(resolver)
        }

        val handlerCode = generatedCode.first()
        assert(handlerCode.contains("class Handler__${handlerName}__1"))
        assert(handlerCode.contains(": RequestHandler<$inputClass?, $outputClass<*>?>"))
        assert(handlerCode.contains("= $handlerName"))
        assert(handlerCode.contains("override suspend fun handleRequest(request: $inputClass?): " +
                "$outputClass<*>?"))

    }

    @Test
    fun `request handler multiple parameters`() {
        val inputClass = "TestInputClass"
        val argClass = "TestArgClass"
        val outputClass = "TestReturnClass"
        val handlerName = "testHandler"
        val funDecl = functionDeclaration {
            setupHandler(handlerName)

            parameter {
                name { "first" }
                typeRef {
                    type {
                        nullability { Nullability.NULLABLE }
                        classDeclaration {
                            packageName { "paramPackage" }
                            qualifiedName { "paramPackage.$inputClass" }
                            classKind { ClassKind.CLASS }
                        }
                    }
                    element {
                        mockkClass(KSClassifierReference::class)
                    }
                }
            }

            parameter {
                name { "second" }
                typeRef {
                    type {
                        nullability { Nullability.NULLABLE }
                        classDeclaration {
                            packageName { "paramPackage" }
                            qualifiedName { "paramPackage.$argClass" }
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
                    nullability { Nullability.NULLABLE }
                    classDeclaration {
                        packageName { "returnPackage" }
                        qualifiedName { "returnPackage.$outputClass" }
                        classKind { ClassKind.CLASS }
                    }
                }
            }
        }

        val generatedCode = generateStringOutput(1) {
            val resolver = mockkClass(Resolver::class)
            every { resolver.getSymbolsWithAnnotation("annotations.RequestHandler") } returns sequenceOf()
            every { resolver.getSymbolsWithAnnotation("annotations.NotificationHandler") } returns sequenceOf()

            RequestHandlerVisitor().visitFunctionDeclaration(funDecl, Unit)
            process(resolver)
        }

        val handlerCode = generatedCode.first()
        assert(handlerCode.contains("class Handler__${handlerName}__1"))
        assert(handlerCode.contains(": RequestHandler<$inputClass?, $outputClass?>"))
        assert(handlerCode.contains("val second: $argClass"))
        assert(handlerCode.contains("= $handlerName"))
        assert(handlerCode.contains("override suspend fun handleRequest(request: $inputClass?): " +
                "$outputClass?"))

    }

    @Test
    fun `request handler function param`() {
        val inputClass = "TestInputClass"
        val argClass = "TestArgClass"
        val outputClass = "TestReturnClass"
        val handlerName = "testHandler"
        val funDecl = functionDeclaration {
            setupHandler(handlerName)

            parameter {
                name { "first" }
                typeRef {
                    type {
                        nullability { Nullability.NULLABLE }
                        classDeclaration {
                            packageName { "paramPackage" }
                            qualifiedName { "paramPackage.$inputClass" }
                            classKind { ClassKind.CLASS }
                        }
                    }
                    element {
                        mockkClass(KSClassifierReference::class)
                    }
                }
            }

            parameter {
                name { "second" }
                typeRef {
                    type {
                        nullability { Nullability.NULLABLE }
                        classDeclaration {
                            packageName { "paramPackage" }
                            qualifiedName { "paramPackage.$argClass" }
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
                    nullability { Nullability.NULLABLE }
                    classDeclaration {
                        packageName { "returnPackage" }
                        qualifiedName { "returnPackage.$outputClass" }
                        classKind { ClassKind.CLASS }
                    }
                }
            }
        }

        val generatedCode = generateStringOutput(1) {
            val resolver = mockkClass(Resolver::class)
            every { resolver.getSymbolsWithAnnotation("annotations.RequestHandler") } returns sequenceOf()
            every { resolver.getSymbolsWithAnnotation("annotations.NotificationHandler") } returns sequenceOf()

            RequestHandlerVisitor().visitFunctionDeclaration(funDecl, Unit)
            process(resolver)
        }

        val handlerCode = generatedCode.first()
        assert(handlerCode.contains("class Handler__${handlerName}__1"))
        assert(handlerCode.contains(": RequestHandler<$inputClass?, $outputClass?>"))
        assert(handlerCode.contains("= $handlerName"))
        assert(handlerCode.contains("override suspend fun handleRequest(request: $inputClass?): " +
                "$outputClass?"))

    }
}


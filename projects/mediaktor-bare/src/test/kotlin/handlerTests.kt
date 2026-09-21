import testUtils.classDeclaration
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassifierReference
import com.google.devtools.ksp.symbol.Nullability
import com.google.devtools.ksp.symbol.Variance
import testUtils.functionDeclaration
import testUtils.generateStringOutput
import io.mockk.every
import io.mockk.mockkClass
import testUtils.argument
import testUtils.classKind
import testUtils.element
import testUtils.name
import testUtils.nullability
import testUtils.packageName
import testUtils.parameter
import testUtils.qualifiedName
import testUtils.returnType
import testUtils.setupHandler
import testUtils.type
import testUtils.typeRef
import testUtils.variance
import kotlin.test.Test

class HandlerTests {
    @Test
    fun `request handler type without modifications`() {
        val inputClass = "TestInputClass"
        val outputClass = "TestReturnClass"
        val handlerName = "testHandler"
        val packageName = "handlers"

        val funDecl = functionDeclaration {
            setupHandler(handlerName, packageName = packageName)

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
            every { resolver.getSymbolsWithAnnotation("annotations.PipelineBehavior") } returns sequenceOf()

            RequestHandlerVisitor().visitFunctionDeclaration(funDecl, Unit)
            process(resolver)
        }

        val handlerCode = generatedCode.first()
        assert(handlerCode.contains("class Handler__${packageName}__${handlerName}"))
        assert(handlerCode.contains("= $handlerName"))
        assert(handlerCode.contains("suspend fun handleRequest(request: $inputClass): $outputClass"))
    }

    @Test
    fun `request handler covariant non-nullable-parametrized return type`() {
        val inputClass = "TestInputClass"
        val outputClass = "TestReturnClass"
        val handlerName = "testHandler"
        val typeParamName = "TypeParam"
        val packageName = "handlers"

        val funDecl = functionDeclaration {
            setupHandler(handlerName, packageName)

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
            every { resolver.getSymbolsWithAnnotation("annotations.PipelineBehavior") } returns sequenceOf()

            RequestHandlerVisitor().visitFunctionDeclaration(funDecl, Unit)
            process(resolver)
        }

        val handlerCode = generatedCode.first()
        assert(handlerCode.contains("class Handler__${packageName}__${handlerName}"))
        assert(handlerCode.contains("= $handlerName"))
        assert(handlerCode.contains("suspend fun handleRequest(request: $inputClass): " +
                "$outputClass<out $typeParamName>?"))

    }

    @Test
    fun `request handler nullable-parametrized return type`() {
        val inputClass = "TestInputClass"
        val outputClass = "TestReturnClass"
        val handlerName = "testHandler"
        val typeParamName = "TypeParam"
        val packageName = "handlers"

        val funDecl = functionDeclaration {
            setupHandler(handlerName, packageName)

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
            every { resolver.getSymbolsWithAnnotation("annotations.PipelineBehavior") } returns sequenceOf()

            RequestHandlerVisitor().visitFunctionDeclaration(funDecl, Unit)
            process(resolver)
        }

        val handlerCode = generatedCode.first()
        assert(handlerCode.contains("class Handler__${packageName}__${handlerName}"))
        assert(handlerCode.contains("= $handlerName"))
        assert(handlerCode.contains("suspend fun handleRequest(request: $inputClass): " +
                "$outputClass<$typeParamName?>?"))

    }

    @Test
    fun `request handler star-parametrized return type`() {
        val inputClass = "TestInputClass"
        val outputClass = "TestReturnClass"
        val handlerName = "testHandler"
        val packageName = "handlers"
        val funDecl = functionDeclaration {
            setupHandler(handlerName, packageName)

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
            every { resolver.getSymbolsWithAnnotation("annotations.PipelineBehavior") } returns sequenceOf()

            RequestHandlerVisitor().visitFunctionDeclaration(funDecl, Unit)
            process(resolver)
        }

        val handlerCode = generatedCode.first()
        assert(handlerCode.contains("class Handler__${packageName}__${handlerName}"))
        assert(handlerCode.contains("= $handlerName"))
        assert(handlerCode.contains("suspend fun handleRequest(request: $inputClass): " +
                "$outputClass<*>?"))

    }

    @Test
    fun `request handler multiple parameters`() {
        val inputClass = "TestInputClass"
        val argClass = "TestArgClass"
        val outputClass = "TestReturnClass"
        val handlerName = "testHandler"
        val packageName = "handlers"
        val funDecl = functionDeclaration {
            setupHandler(handlerName, packageName)

            parameter {
                name { "first" }
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
            every { resolver.getSymbolsWithAnnotation("annotations.PipelineBehavior") } returns sequenceOf()

            RequestHandlerVisitor().visitFunctionDeclaration(funDecl, Unit)
            process(resolver)
        }

        val handlerCode = generatedCode.first()
        assert(handlerCode.contains("class Handler__${packageName}__${handlerName}"))
        assert(handlerCode.contains("val second: $argClass"))
        assert(handlerCode.contains("= $handlerName"))
        assert(handlerCode.contains("suspend fun handleRequest(request: $inputClass): " +
                "$outputClass?"))

    }
}

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassifierReference
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.Nullability
import io.mockk.every
import io.mockk.mockkClass
import kotlin.test.Test

fun KSFunctionDeclaration.setupTypeTest(handlerName: String) {
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
                    qualifiedName { "annotations.HandlerLifespan.SINGLE" }
                    classKind { ClassKind.ENUM_ENTRY }

                    every { this@classDeclaration.toString() } returns "HandlerLifespan.SINGLE"
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

class HandlerTest {
    @Test
    fun `request handler type without modifications`() {
        val inputClass = "TestInputClass"
        val outputClass = "TestReturnClass"
        val handlerName = "testHandler"

        val funDecl = functionDeclaration {
            setupTypeTest(handlerName)

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

            HandlerVisitor().visitFunctionDeclaration(funDecl, Unit)
            process(resolver)
        }

        val handlerCode = generatedCode.first()
        assert(handlerCode.contains("class Handler__${handlerName}__1"))
        assert(handlerCode.contains(": RequestHandler<$inputClass, $outputClass>"))
        assert(handlerCode.contains("= $handlerName"))
        assert(handlerCode.contains("override suspend fun handleRequest(request: $inputClass): $outputClass"))
        print(handlerCode)
    }

    @Test
    fun `request handler nullable type`() {
        val inputClass = "TestInputClass"
        val outputClass = "TestReturnClass"
        val handlerName = "testHandler"

        val funDecl = functionDeclaration {
            setupTypeTest(handlerName)

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

            HandlerVisitor().visitFunctionDeclaration(funDecl, Unit)
            process(resolver)
        }

        val handlerCode = generatedCode.first()
        assert(handlerCode.contains("class Handler__${handlerName}__1"))
        assert(handlerCode.contains(": RequestHandler<$inputClass?, $outputClass?>"))
        assert(handlerCode.contains("= $handlerName"))
        assert(handlerCode.contains("override suspend fun handleRequest(request: $inputClass?): $outputClass?"))
        print(handlerCode)
    }
}


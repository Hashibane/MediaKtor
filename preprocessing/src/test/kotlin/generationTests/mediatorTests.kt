package generationTests

import annotations.NotificationParallel
import argument
import classDeclaration
import classKind
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassifierReference
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.Nullability
import com.google.devtools.ksp.symbol.Variance
import com.squareup.kotlinpoet.TypeName
import element
import functionDeclaration
import generateStringOutput
import io.mockk.every
import io.mockk.mockkClass
import isSuspendFunctionType
import metadata.NotificationHandlerMetadata
import metadata.PipelineMetadata
import name
import nullability
import packageName
import parameter
import qualifiedName
import returnType
import setupHandler
import setupHandlerReturn
import superType
import type
import typeParameter
import typeRef
import variance
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
            every { resolver.getSymbolsWithAnnotation("annotations.PipelineBehavior") } returns sequenceOf()

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
            every { resolver.getSymbolsWithAnnotation("annotations.PipelineBehavior") } returns sequenceOf()

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
            every { resolver.getSymbolsWithAnnotation("annotations.PipelineBehavior") } returns sequenceOf()

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
        assert(mediatorCode.contains(
            """handler__${handlerOneName.lowercase()}__1().handleRequest(command)
            |          handler__${handlerTwoName.lowercase()}__2().handleRequest(command)
        """.trimMargin()))
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
            every { resolver.getSymbolsWithAnnotation("annotations.PipelineBehavior") } returns sequenceOf()

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
        assert(!mediatorCode.contains(
            """handler__${handlerOneName.lowercase()}__1().handleRequest(command)
            |          handler__${handlerTwoName.lowercase()}__2().handleRequest(command)
        """.trimMargin()))
    }

    @Test
    fun `multiple parallel notifiers with reversed order`() {
        val inputOneClass = "TestInputClass1"
        val handlerOneName = "testHandler1"

        val handlerOne = functionDeclaration {
            setupHandlerReturn(handlerOneName,
                notificationHandlerData = NotificationHandlerMetadata(NotificationParallel.PARALLEL, 2))

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
                notificationHandlerData = NotificationHandlerMetadata(NotificationParallel.PARALLEL, 1))

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
            every { resolver.getSymbolsWithAnnotation("annotations.PipelineBehavior") } returns sequenceOf()

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

        assert(mediatorCode.indexOf(
            "${handlerOneName.lowercase()}__1()") > mediatorCode.indexOf("${handlerTwoName.lowercase()}__1()"))

        assert(mediatorCode.contains(
            """launch {
                |            handler__${handlerOneName.lowercase()}__1().handleRequest(command)
                |          }
        """.trimMargin()))
        assert(mediatorCode.contains(
            """launch {
                |            handler__${handlerTwoName.lowercase()}__2().handleRequest(command)
                |          }
        """.trimMargin()))
    }

    /*
    input:
        InputType1 <: InputType2
        rq1 -> RequestHandler(paramPackage.InputType1) : InputType2
        rq2 -> RequestHandler(paramPackage.InputType1?) : InputType2
        rq3 -> RequestHandler(paramPackage.InputType2) : InputType2
        rq4 -> RequestHandler(paramPackage.InputType2?) : InputType2

        pl4 -> PipelineBehavior(paramPackage.InputType2?): InputType2
        pl3 -> PipelineBehaviour(paramPackage.InputType2): InputType2
        pl2 -> PipelineBehavior(paramPackage.InputType1?): InputType2
        pl1 -> PipelineBehavior(paramPackage.InputType1): InputType2

        [InputType1 <: InputType2, InputType1 <: InputType1?, InputType2 <: InputType2?, InputType1? <: InputType2?]
    output:
        invoke()
            is InputType1 -> pl1 pl2 pl3 pl4 rq1
            is InputType1? -> pl2 pl4 rq2
            is InputType2 -> pl3 pl4 rq3
            is InputType2? -> pl4 rq4
     */
    @Test
    fun `multiple pipeline behaviors with complex type hierarchy`() {
        val inputPackage = "paramPackage"
        val inputOneType = "InputType1"
        val inputTwoType = "InputType2"

        val returnPackage = "returnPackage"
        val returnType = "ReturnType"

        val inputTwoClass: KSTypeReference.(Nullability) -> Unit = { nullability ->
            type {
                nullability { nullability }
                classDeclaration {
                    packageName { inputPackage }
                    qualifiedName { "$inputPackage.$inputTwoType" }
                    classKind { ClassKind.CLASS }
                }
            }
        }

        val inputOneClass: KSTypeReference.(Nullability) -> Unit = { nullability ->
            type {
                nullability { nullability }
                classDeclaration {
                    packageName { inputPackage }
                    qualifiedName { "$inputPackage.$inputOneType" }
                    classKind { ClassKind.CLASS }

                    superType {
                        inputTwoClass(Nullability.NOT_NULL)
                    }
                }
            }
        }



        // -- handlers

        val returnTypePart: KSTypeReference.() -> Unit = {
            type {
                nullability { Nullability.NULLABLE }
                classDeclaration {
                    packageName { returnPackage }
                    qualifiedName { "$returnPackage.$returnType" }
                    classKind { ClassKind.CLASS }
                }
            }
        }

        val handlerOneName = "handlerOne"
        val handlerOne = functionDeclaration {
            setupHandler(handlerOneName)

            parameter {
                typeRef {
                    inputOneClass(Nullability.NOT_NULL)
                }
            }

            returnType {
                returnTypePart()
            }
        }


        val handlerTwoName = "handlerTwo"
        val handlerTwo = functionDeclaration {
            setupHandler(handlerTwoName)

            parameter {
                typeRef {
                    inputOneClass(Nullability.NULLABLE)
                }
            }

            returnType {
                returnTypePart()
            }
        }

        val handlerThreeName = "handlerThree"
        val handlerThree = functionDeclaration {
            setupHandler(handlerThreeName)

            parameter {
                typeRef {
                    inputTwoClass(Nullability.NOT_NULL)
                }
            }

            returnType {
                returnTypePart()
            }
        }

        val handlerFourName = "handlerFour"
        val handlerFour = functionDeclaration {
            setupHandler(handlerFourName)

            parameter {
                typeRef {
                    inputTwoClass(Nullability.NULLABLE)
                }
            }

            returnType {
                returnTypePart()
            }
        }

        // -- pipelines

        val pipelineOneName = "pipelineOne"
        val pipelineOne = functionDeclaration {
            setupHandler(pipelineOneName, additionalData = PipelineMetadata(mockkClass(TypeName::class), 0))

            parameter {
                name { "request" }

                typeRef {
                    inputOneClass(Nullability.NOT_NULL)
                }
            }

            parameter {
                name { "next" }

                typeRef {
                    type {
                        nullability { Nullability.NOT_NULL }
                        isSuspendFunctionType { true }

                        argument {
                            variance { Variance.INVARIANT }

                            typeRef {
                                inputOneClass(Nullability.NOT_NULL)
                            }
                        }

                        argument {
                            variance { Variance.INVARIANT }

                            typeRef {
                                returnTypePart()
                            }
                        }

                        functionDeclaration {
                            typeParameter {}

                            typeParameter {
                                name { "$returnPackage.$returnType" }
                            }

                            parameter {
                                typeRef {
                                    inputOneClass(Nullability.NOT_NULL)
                                }
                            }

                            returnType {
                                returnTypePart()
                            }
                        }
                    }
                }

            }

            returnType {
                returnTypePart()
            }
        }

        val pipelineTwoName = "pipelineTwo"
        val pipelineTwo = functionDeclaration {
            setupHandler(pipelineTwoName, additionalData = PipelineMetadata(mockkClass(TypeName::class), 2))

            parameter {
                name { "request" }

                typeRef {
                    inputOneClass(Nullability.NULLABLE)
                }
            }

            parameter {
                name { "next" }

                typeRef {
                    type {
                        nullability { Nullability.NOT_NULL }
                        isSuspendFunctionType { true }

                        argument {
                            variance { Variance.INVARIANT }

                            typeRef {
                                inputOneClass(Nullability.NULLABLE)
                            }
                        }

                        argument {
                            variance { Variance.INVARIANT }

                            typeRef {
                                returnTypePart()
                            }
                        }

                        functionDeclaration {
                            typeParameter {}

                            typeParameter {
                                name { "$returnPackage.$returnType" }
                            }

                            parameter {
                                typeRef {
                                    inputOneClass(Nullability.NULLABLE)
                                }
                            }

                            returnType {
                                returnTypePart()
                            }
                        }
                    }
                }

            }

            returnType {
                returnTypePart()
            }
        }

        val pipelineThreeName = "pipelineThree"
        val pipelineThree = functionDeclaration {
            setupHandler(pipelineThreeName, additionalData = PipelineMetadata(mockkClass(TypeName::class), 1))

            parameter {
                name { "request" }

                typeRef {
                    inputTwoClass(Nullability.NOT_NULL)
                }
            }

            parameter {
                name { "next" }

                typeRef {
                    type {
                        nullability { Nullability.NOT_NULL }
                        isSuspendFunctionType { true }

                        argument {
                            variance { Variance.INVARIANT }

                            typeRef {
                                inputTwoClass(Nullability.NOT_NULL)
                            }
                        }

                        argument {
                            variance { Variance.INVARIANT }

                            typeRef {
                                returnTypePart()
                            }
                        }

                        functionDeclaration {
                            typeParameter {}

                            typeParameter {
                                name { "$returnPackage.$returnType" }
                            }

                            parameter {
                                typeRef {
                                    inputTwoClass(Nullability.NOT_NULL)
                                }
                            }

                            returnType {
                                returnTypePart()
                            }
                        }
                    }
                }

            }

            returnType {
                returnTypePart()
            }
        }

        val pipelineFourName = "pipelineFour"
        val pipelineFour = functionDeclaration {
            setupHandler(pipelineFourName, additionalData = PipelineMetadata(mockkClass(TypeName::class), Int.MIN_VALUE))

            parameter {
                name { "request" }

                typeRef {
                    inputTwoClass(Nullability.NULLABLE)
                }
            }

            parameter {
                name { "next" }

                typeRef {
                    type {
                        nullability { Nullability.NOT_NULL }
                        isSuspendFunctionType { true }

                        argument {
                            variance { Variance.INVARIANT }

                            typeRef {
                                inputTwoClass(Nullability.NULLABLE)
                            }
                        }

                        argument {
                            variance { Variance.INVARIANT }

                            typeRef {
                                returnTypePart()
                            }
                        }

                        functionDeclaration {
                            typeParameter {}

                            typeParameter {
                                name { "$returnPackage.$returnType" }
                            }

                            parameter {
                                typeRef {
                                    inputTwoClass(Nullability.NULLABLE)
                                }
                            }

                            returnType {
                                returnTypePart()
                            }
                        }
                    }
                }

            }

            returnType {
                returnTypePart()
            }
        }

        val generatedCode = generateStringOutput(1) {
            val resolver = mockkClass(Resolver::class)
            every { resolver.getSymbolsWithAnnotation("annotations.RequestHandler") } returns sequenceOf()
            every { resolver.getSymbolsWithAnnotation("annotations.NotificationHandler") } returns sequenceOf()
            every { resolver.getSymbolsWithAnnotation("annotations.PipelineBehavior") } returns sequenceOf()

            RequestHandlerVisitor().visitFunctionDeclaration(handlerOne, Unit)
            RequestHandlerVisitor().visitFunctionDeclaration(handlerTwo, Unit)
            RequestHandlerVisitor().visitFunctionDeclaration(handlerThree, Unit)
            RequestHandlerVisitor().visitFunctionDeclaration(handlerFour, Unit)

            PipelineHandlerVisitor().visitFunctionDeclaration(pipelineOne, Unit)
            PipelineHandlerVisitor().visitFunctionDeclaration(pipelineTwo, Unit)
            PipelineHandlerVisitor().visitFunctionDeclaration(pipelineThree, Unit)
            PipelineHandlerVisitor().visitFunctionDeclaration(pipelineFour, Unit)
            process(resolver)
        }

        val mediatorCode = generatedCode.drop(2).first()
        assert(mediatorCode.contains(": Mediator"))
        assert(mediatorCode.contains("override suspend operator fun <T : Any> invoke"))
        assert(mediatorCode.contains(": suspend () -> Handler__${handlerOneName}__1"))
        assert(mediatorCode.contains(": suspend () -> Handler__${handlerTwoName}__2"))
        assert(mediatorCode.contains(": suspend () -> Handler__${handlerThreeName}__3"))
        assert(mediatorCode.contains(": suspend () -> Handler__${handlerFourName}__4"))
        assert(mediatorCode.contains(": suspend () -> Handler__${pipelineOneName}__5"))
        assert(mediatorCode.contains(": suspend () -> Handler__${pipelineTwoName}__6"))
        assert(mediatorCode.contains(": suspend () -> Handler__${pipelineThreeName}__7"))
        assert(mediatorCode.contains(": suspend () -> Handler__${pipelineFourName}__8"))
        assert(mediatorCode.contains("is $inputOneType ->"))
        assert(mediatorCode.contains("is $inputOneType? ->"))
        assert(mediatorCode.contains("is $inputTwoType ->"))
        assert(mediatorCode.contains("is $inputTwoType? ->"))

        assert(
            mediatorCode.contains(
                """is InputType1 -> {
      handler__${pipelineFourName.lowercase()}__8().handleRequest(command) {
        handler__${pipelineOneName.lowercase()}__5().handleRequest(command) {
          handler__${pipelineThreeName.lowercase()}__7().handleRequest(command) {
            handler__${pipelineTwoName.lowercase()}__6().handleRequest(command) {
              handler__${handlerOneName.lowercase()}__1().handleRequest(command)
            }""".trimMargin()
            )
        )

        assert(
            mediatorCode.contains(
                """is InputType2 -> {
      handler__${pipelineFourName.lowercase()}__8().handleRequest(command) {
        handler__${pipelineThreeName.lowercase()}__7().handleRequest(command) {
          handler__${handlerThreeName.lowercase()}__3().handleRequest(command)
        }
      }
    }""".trimMargin()
            )
        )

        assert(
            mediatorCode.contains(
                """is InputType1? -> {
      handler__${pipelineFourName.lowercase()}__8().handleRequest(command) {
        handler__${pipelineTwoName.lowercase()}__6().handleRequest(command) {
          handler__${handlerTwoName.lowercase()}__2().handleRequest(command)
        }
      }
    }""".trimMargin()
            )
        )

        assert(
            mediatorCode.contains(
                """is InputType2? -> {
      handler__${pipelineFourName.lowercase()}__8().handleRequest(command) {
        handler__${handlerFourName.lowercase()}__4().handleRequest(command)
      }
    }""".trimMargin()
            )
        )

        assert(mediatorCode.indexOf(
            "is $inputOneType ->") < mediatorCode.indexOf("is $inputTwoType ->"))
        assert(mediatorCode.indexOf(
            "is $inputOneType ->") < mediatorCode.indexOf("is $inputOneType? ->"))
        assert(mediatorCode.indexOf(
            "is $inputOneType ->") < mediatorCode.indexOf("is $inputTwoType? ->"))

        assert(mediatorCode.indexOf(
            "is $inputOneType? ->") < mediatorCode.indexOf("is $inputTwoType? ->"))

        assert(mediatorCode.indexOf(
            "is $inputTwoType ->") < mediatorCode.indexOf("is $inputTwoType? ->"))
    }
}

/*
   input:
       InputType1 <: InputType2
       rq1 -> RequestHandler(paramPackage.InputType1) : InputType2
       rq2 -> RequestHandler(paramPackage.InputType1?) : InputType2
       rq3 -> RequestHandler(paramPackage.InputType2) : InputType2
       rq4 -> RequestHandler(paramPackage.InputType2?) : InputType2

       nf1 -> NotificationHandler(paramPackage.InputType2?)
       nf2 -> NotificationHandler(paramPackage.InputType1?)

       pl4 -> PipelineBehavior(paramPackage.InputType2?): InputType2
       pl3 -> PipelineBehaviour(paramPackage.InputType2?): Unit
       pl2 -> PipelineBehavior(paramPackage.InputType1?): Unit
       pl1 -> PipelineBehavior(paramPackage.InputType1): InputType2

       [InputType1 <: InputType2, InputType1 <: InputType1?, InputType2 <: InputType2?, InputType1? <: InputType2?]
   output:
       invoke()
           is InputType1 -> pl1 pl4 rq1
           is InputType1? -> pl4 rq2
           is InputType2 -> pl4 rq3
           is InputType2? -> pl4 rq4
       publish()
           is InputType1? -> pl2 pl3 nf1 nf2
           is InputType2? -> pl3 nf1
*/
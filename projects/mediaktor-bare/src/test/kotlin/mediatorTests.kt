import annotations.NotificationParallel
import annotations.PipelineTarget
import testUtils.classDeclaration
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.TypeName
import testUtils.functionDeclaration
import testUtils.generateStringOutput
import io.mockk.every
import io.mockk.mockkClass
import preprocessing.metadata.NotificationHandlerMetadata
import preprocessing.metadata.PipelineMetadata
import testUtils.*
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
        assert(mediatorCode.contains("override suspend fun send"))
        assert(mediatorCode.contains(": suspend () -> Handler__${handlerOneName}__1"))
        assert(mediatorCode.contains(": suspend () -> Handler__${handlerTwoName}__2"))
        assert(mediatorCode.contains("is $inputOneClass ->"))
        assert(mediatorCode.contains("is $inputTwoClass ->"))
    }

    @Test
    fun `multiple notifiers with same input type test`() {
        val inputOneClass = "TestInputClass1"
        val handlerOneName = "testHandler1"

        val paramType: KSTypeReference.() -> Unit = {
            type {
                nullability { Nullability.NOT_NULL }
                classDeclaration {
                    packageName { "paramPackage" }
                    qualifiedName { "paramPackage.$inputOneClass" }
                    classKind { ClassKind.CLASS }
                }
            }
        }

        val handlerOne = functionDeclaration {
            setupHandlerReturn(
                handlerOneName,
                notificationHandlerData = NotificationHandlerMetadata(NotificationParallel.SEQUENTIAL, 1)
            )

            parameter {
                typeRef {
                    paramType()
                    element {
                        mockkClass(KSClassifierReference::class)
                    }
                }
            }
        }

        val handlerTwoName = "testHandler2"

        val handlerTwo = functionDeclaration {
            setupHandlerReturn(
                handlerTwoName,
                notificationHandlerData = NotificationHandlerMetadata(NotificationParallel.SEQUENTIAL, 1)
            )

            parameter {
                typeRef {
                    paramType()
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
        assert(mediatorCode.contains("override suspend fun publish"))
        assert(mediatorCode.contains(": suspend () -> Handler__${handlerOneName}__1"))
        assert(mediatorCode.contains(": suspend () -> Handler__${handlerTwoName}__2"))
        assert(mediatorCode.contains("is $inputOneClass ->"))
        assert(
            mediatorCode.contains(
                """is TestInputClass1 -> {
        coroutineScope {
          handler__${handlerOneName.lowercase()}__1().handleRequest(notification)
          handler__${handlerTwoName.lowercase()}__2().handleRequest(notification)
        }
      }""".trimMargin()
            )
        )
    }

    @Test
    fun `multiple notifiers with different request types test`() {
        val inputOneClass = "TestInputClass1"
        val handlerOneName = "testHandler1"

        val handlerOne = functionDeclaration {
            setupHandlerReturn(
                handlerOneName,
                notificationHandlerData = NotificationHandlerMetadata(NotificationParallel.SEQUENTIAL, 0)
            )

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
            setupHandlerReturn(
                handlerTwoName,
                notificationHandlerData = NotificationHandlerMetadata(NotificationParallel.SEQUENTIAL, 1)
            )

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
        assert(mediatorCode.contains("override suspend fun publish"))
        assert(mediatorCode.contains(": suspend () -> Handler__${handlerOneName}__1"))
        assert(mediatorCode.contains(": suspend () -> Handler__${handlerTwoName}__2"))
        assert(mediatorCode.contains("is $inputOneClass ->"))
        assert(mediatorCode.contains("is $inputTwoClass ->"))
        assert(mediatorCode.contains("handler__${handlerOneName.lowercase()}__1().handleRequest(notification)"))
        assert(mediatorCode.contains("handler__${handlerTwoName.lowercase()}__2().handleRequest(notification)"))
        assert(
            !mediatorCode.contains(
                """handler__${handlerOneName.lowercase()}__1().handleRequest(notification)
            |          handler__${handlerTwoName.lowercase()}__2().handleRequest(notification)
        """.trimMargin()
            )
        )
    }

    @Test
    fun `multiple parallel notifiers with reversed order`() {
        val inputOneClass = "TestInputClass1"
        val handlerOneName = "testHandler1"

        val handlerOne = functionDeclaration {
            setupHandlerReturn(
                handlerOneName,
                notificationHandlerData = NotificationHandlerMetadata(NotificationParallel.PARALLEL, 2)
            )

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
            setupHandlerReturn(
                handlerTwoName,
                notificationHandlerData = NotificationHandlerMetadata(NotificationParallel.PARALLEL, 1)
            )

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
        assert(mediatorCode.contains("override suspend fun publish"))
        assert(mediatorCode.contains(": suspend () -> Handler__${handlerOneName}__1"))
        assert(mediatorCode.contains(": suspend () -> Handler__${handlerTwoName}__2"))
        assert(mediatorCode.contains("is $inputOneClass ->"))
        assert(mediatorCode.contains("is $inputTwoClass ->"))

        assert(
            mediatorCode.indexOf(
                "${handlerOneName.lowercase()}__1()"
            ) > mediatorCode.indexOf("${handlerTwoName.lowercase()}__1()")
        )

        assert(
            mediatorCode.contains(
                """launch {
                |            handler__${handlerOneName.lowercase()}__1().handleRequest(notification)
                |          }
        """.trimMargin()
            )
        )
        assert(
            mediatorCode.contains(
                """launch {
                |            handler__${handlerTwoName.lowercase()}__2().handleRequest(notification)
                |          }
        """.trimMargin()
            )
        )
    }

    /*
    input:
        InputType1 <: InputType2 <: InputType4
        InputType1 <: InputType3 <: InputType4

        rq1 -> RequestHandler(paramPackage.InputType1) : InputType2
        rq2 -> RequestHandler(paramPackage.InputType2) : InputType2
        rq3 -> RequestHandler(paramPackage.InputType3) : InputType2
        rq4 -> RequestHandler(paramPackage.InputType4) : InputType2

        pl4 -> PipelineBehavior(paramPackage.InputType4): InputType2
        pl3 -> PipelineBehaviour(paramPackage.InputType3): InputType2
        pl2 -> PipelineBehavior(paramPackage.InputType2): InputType2
        pl1 -> PipelineBehavior(paramPackage.InputType1): InputType2

    output:
        send()
            is InputType1 -> pl1 pl2 pl3 pl4 rq1
            is InputType2 -> pl2 pl4 rq2
            is InputType3 -> pl3 pl4 rq3
            is InputType4 -> pl4 rq4
     */
    @Test
    fun `multiple pipeline behaviors with complex type hierarchy`() {
        val inputPackage = "paramPackage"
        val inputOneType = "InputType1"
        val inputTwoType = "InputType2"
        val inputThreeType = "InputType3"
        val inputFourType = "InputType4"

        val returnPackage = "returnPackage"
        val returnType = "ReturnType"

        val inputFourClass: KSTypeReference.() -> Unit = {
            type {
                nullability { Nullability.NOT_NULL }
                classDeclaration {
                    packageName { inputPackage }
                    qualifiedName { "$inputPackage.$inputFourType" }
                    classKind { ClassKind.CLASS }
                }
            }
        }

        val inputThreeClass: KSTypeReference.() -> Unit = {
            type {
                nullability { Nullability.NOT_NULL }
                classDeclaration {
                    packageName { inputPackage }
                    qualifiedName { "$inputPackage.$inputThreeType" }
                    classKind { ClassKind.CLASS }

                    superType {
                        inputFourClass()
                    }
                }
            }
        }

        val inputTwoClass: KSTypeReference.() -> Unit = {
            type {
                nullability { Nullability.NOT_NULL }
                classDeclaration {
                    packageName { inputPackage }
                    qualifiedName { "$inputPackage.$inputTwoType" }
                    classKind { ClassKind.CLASS }

                    superType {
                        inputFourClass()
                    }
                }
            }
        }

        val inputOneClass: KSTypeReference.() -> Unit = {
            type {
                nullability { Nullability.NOT_NULL }
                classDeclaration {
                    packageName { inputPackage }
                    qualifiedName { "$inputPackage.$inputOneType" }
                    classKind { ClassKind.CLASS }

                    superType {
                        inputTwoClass()
                    }

                    superType {
                        inputThreeClass()
                    }

                    superType {
                        inputFourClass()
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
                    inputOneClass()
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
                    inputTwoClass()
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
                    inputThreeClass()
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
                    inputFourClass()
                }
            }

            returnType {
                returnTypePart()
            }
        }

        // -- pipelines

        val pipelineOneName = "pipelineOne"
        val pipelineOne = functionDeclaration {
            setupHandler(
                pipelineOneName,
                additionalData = PipelineMetadata(mockkClass(TypeName::class), 0, PipelineTarget.REQUEST_MATCH)
            )

            parameter {
                name { "request" }

                typeRef {
                    inputOneClass()
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
                                returnTypePart()
                            }
                        }

                        functionDeclaration {
                            typeParameter {
                                name { "$returnPackage.$returnType" }
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
            setupHandler(
                pipelineTwoName,
                additionalData = PipelineMetadata(mockkClass(TypeName::class), 2, PipelineTarget.REQUEST_MATCH)
            )

            parameter {
                name { "request" }

                typeRef {
                    inputTwoClass()
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
                                returnTypePart()
                            }
                        }

                        functionDeclaration {
                            typeParameter {
                                name { "$returnPackage.$returnType" }
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
            setupHandler(
                pipelineThreeName,
                additionalData = PipelineMetadata(mockkClass(TypeName::class), 1, PipelineTarget.REQUEST_MATCH)
            )

            parameter {
                name { "request" }

                typeRef {
                    inputThreeClass()
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
                                returnTypePart()
                            }
                        }

                        functionDeclaration {
                            typeParameter {
                                name { "$returnPackage.$returnType" }
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
            setupHandler(
                pipelineFourName,
                additionalData = PipelineMetadata(
                    mockkClass(TypeName::class),
                    Int.MIN_VALUE,
                    PipelineTarget.REQUEST_MATCH
                )
            )

            parameter {
                name { "request" }

                typeRef {
                    inputFourClass()
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
                                returnTypePart()
                            }
                        }

                        functionDeclaration {
                            typeParameter {
                                name { "$returnPackage.$returnType" }
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
        assert(mediatorCode.contains("override suspend fun send"))
        assert(mediatorCode.contains(": suspend () -> Handler__${handlerOneName}__1"))
        assert(mediatorCode.contains(": suspend () -> Handler__${handlerTwoName}__2"))
        assert(mediatorCode.contains(": suspend () -> Handler__${handlerThreeName}__3"))
        assert(mediatorCode.contains(": suspend () -> Handler__${handlerFourName}__4"))
        assert(mediatorCode.contains(": suspend () -> Handler__${pipelineOneName}__5"))
        assert(mediatorCode.contains(": suspend () -> Handler__${pipelineTwoName}__6"))
        assert(mediatorCode.contains(": suspend () -> Handler__${pipelineThreeName}__7"))
        assert(mediatorCode.contains(": suspend () -> Handler__${pipelineFourName}__8"))
        assert(mediatorCode.contains("is $inputOneType ->"))
        assert(mediatorCode.contains("is $inputTwoType ->"))
        assert(mediatorCode.contains("is $inputThreeType ->"))
        assert(mediatorCode.contains("is $inputFourType ->"))

        assert(
            mediatorCode.contains(
                """is InputType1 -> {
      handler__${pipelineFourName.lowercase()}__8().handleRequest(message) {
        handler__${pipelineOneName.lowercase()}__5().handleRequest(message) {
          handler__${pipelineThreeName.lowercase()}__7().handleRequest(message) {
            handler__${pipelineTwoName.lowercase()}__6().handleRequest(message) {
              handler__${handlerOneName.lowercase()}__1().handleRequest(message)
            }""".trimMargin()
            )
        )

        assert(
            mediatorCode.contains(
                """is InputType3 -> {
      handler__${pipelineFourName.lowercase()}__8().handleRequest(message) {
        handler__${pipelineThreeName.lowercase()}__7().handleRequest(message) {
          handler__${handlerThreeName.lowercase()}__3().handleRequest(message)
        }
      }
    }""".trimMargin()
            )
        )

        assert(
            mediatorCode.contains(
                """is InputType2 -> {
      handler__${pipelineFourName.lowercase()}__8().handleRequest(message) {
        handler__${pipelineTwoName.lowercase()}__6().handleRequest(message) {
          handler__${handlerTwoName.lowercase()}__2().handleRequest(message)
        }
      }
    }""".trimMargin()
            )
        )

        assert(
            mediatorCode.contains(
                """is InputType4 -> {
      handler__${pipelineFourName.lowercase()}__8().handleRequest(message) {
        handler__${handlerFourName.lowercase()}__4().handleRequest(message)
      }
    }""".trimMargin()
            )
        )

        assert(
            mediatorCode.indexOf(
                "is $inputOneType ->"
            ) < mediatorCode.indexOf("is $inputTwoType ->")
        )
        assert(
            mediatorCode.indexOf(
                "is $inputOneType ->"
            ) < mediatorCode.indexOf("is $inputThreeType ->")
        )
        assert(
            mediatorCode.indexOf(
                "is $inputOneType ->"
            ) < mediatorCode.indexOf("is $inputFourType ->")
        )

        assert(
            mediatorCode.indexOf(
                "is $inputTwoType ->"
            ) < mediatorCode.indexOf("is $inputFourType ->")
        )

        assert(
            mediatorCode.indexOf(
                "is $inputThreeType ->"
            ) < mediatorCode.indexOf("is $inputFourType ->")
        )
    }

    /*
    input:
       InputType1 <: InputType2 <: InputType4
       InputType1 <: InputType3 <: InputType4

       rq1 -> RequestHandler(paramPackage.InputType1) : InputType2
       rq2 -> RequestHandler(paramPackage.InputType2) : Unit
       rq3 -> RequestHandler(paramPackage.InputType3) : InputType2
       rq4 -> RequestHandler(paramPackage.InputType4) : InputType2

       nf1 -> NotificationHandler(paramPackage.InputType2)
       nf2 -> NotificationHandler(paramPackage.InputType4)

       pl5 -> PipelineBehavior(paramPackage.InputType2) (NOTIFICATIONS)
       pl4 -> PipelineBehavior(paramPackage.InputType4) (REQUESTS)
       pl3 -> PipelineBehaviour(paramPackage.InputType4) (BOTH)
       pl2 -> PipelineBehavior(paramPackage.InputType2) (REQUESTS)
       pl1 -> PipelineBehavior(paramPackage.InputType1) (REQUESTS)

    output:
       invoke()
           is InputType1 -> pl1 pl3 pl4 rq1
           is InputType2 -> pl2 pl3 rq2
           is InputType3 -> pl3 pl4 rq3
           is InputType4 -> pl3 pl4 rq4
       publish()
           is InputType2 -> pl3 pl5 nf1
           is InputType4 -> pl3 nf2
    */
    @Test
    fun `multiple pipeline behaviors with complex type hierarchy and notification handlers`() {
        val inputPackage = "paramPackage"
        val inputOneType = "InputType1"
        val inputTwoType = "InputType2"
        val inputThreeType = "InputType3"
        val inputFourType = "InputType4"

        val returnPackage = "returnPackage"
        val returnType = "ReturnType"

        val inputFourClass: KSTypeReference.() -> Unit = {
            type {
                nullability { Nullability.NOT_NULL }
                classDeclaration {
                    packageName { inputPackage }
                    qualifiedName { "$inputPackage.$inputFourType" }
                    classKind { ClassKind.CLASS }
                }
            }
        }

        val inputThreeClass: KSTypeReference.() -> Unit = {
            type {
                nullability { Nullability.NOT_NULL }
                classDeclaration {
                    packageName { inputPackage }
                    qualifiedName { "$inputPackage.$inputThreeType" }
                    classKind { ClassKind.CLASS }

                    superType {
                        inputFourClass()
                    }
                }
            }
        }

        val inputTwoClass: KSTypeReference.() -> Unit = {
            type {
                nullability { Nullability.NOT_NULL }
                classDeclaration {
                    packageName { inputPackage }
                    qualifiedName { "$inputPackage.$inputTwoType" }
                    classKind { ClassKind.CLASS }

                    superType {
                        inputFourClass()
                    }
                }
            }
        }

        val inputOneClass: KSTypeReference.() -> Unit = {
            type {
                nullability { Nullability.NOT_NULL }
                classDeclaration {
                    packageName { inputPackage }
                    qualifiedName { "$inputPackage.$inputOneType" }
                    classKind { ClassKind.CLASS }

                    superType {
                        inputTwoClass()
                    }

                    superType {
                        inputThreeClass()
                    }

                    superType {
                        inputFourClass()
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

        val unitReturnType: KSTypeReference.() -> Unit = {
            type {
                nullability { Nullability.NOT_NULL }
                classDeclaration {
                    packageName { "kotlin" }
                    qualifiedName { "kotlin.Unit" }
                    classKind { ClassKind.OBJECT }
                }
            }
        }

        val handlerOneName = "handlerOne"
        val handlerOne = functionDeclaration {
            setupHandler(handlerOneName)

            parameter {
                typeRef {
                    inputOneClass()
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
                    inputTwoClass()
                }
            }

            returnType {
                unitReturnType()
            }
        }

        val handlerThreeName = "handlerThree"
        val handlerThree = functionDeclaration {
            setupHandler(handlerThreeName)

            parameter {
                typeRef {
                    inputThreeClass()
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
                    inputFourClass()
                }
            }

            returnType {
                returnTypePart()
            }
        }

        // -- notifications

        val notificationOneName = "notificationOne"
        val notificationOne = functionDeclaration {
            setupHandler(
                notificationOneName,
                additionalData = NotificationHandlerMetadata(NotificationParallel.PARALLEL, 2)
            )

            parameter {
                typeRef {
                    inputTwoClass()
                }
            }

            returnType {
                unitReturnType()
            }
        }


        val notificationTwoName = "notificationTwo"
        val notificationTwo = functionDeclaration {
            setupHandler(
                notificationTwoName,
                additionalData = NotificationHandlerMetadata(NotificationParallel.SEQUENTIAL, 1)
            )

            parameter {
                typeRef {
                    inputFourClass()
                }
            }

            returnType {
                unitReturnType()
            }
        }

        // -- pipelines

        val anyReturnType: KSTypeReference.() -> Unit = {
            type {
                nullability { Nullability.NULLABLE }
                classDeclaration {
                    packageName { "kotlin" }
                    qualifiedName { "kotlin.Any" }
                    classKind { ClassKind.CLASS }
                }
            }
        }

        val pipelineOneName = "pipelineOne"
        val pipelineOne = functionDeclaration {
            setupHandler(
                pipelineOneName,
                additionalData = PipelineMetadata(mockkClass(TypeName::class), 0, PipelineTarget.REQUEST_MATCH)
            )

            parameter {
                name { "request" }

                typeRef {
                    inputOneClass()
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
                                returnTypePart()
                            }
                        }

                        functionDeclaration {

                            typeParameter {
                                name { "$returnPackage.$returnType" }
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
            setupHandler(
                pipelineTwoName,
                additionalData = PipelineMetadata(mockkClass(TypeName::class), 2, PipelineTarget.REQUEST_MATCH)
            )

            parameter {
                name { "request" }

                typeRef {
                    inputTwoClass()
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
                                unitReturnType()
                            }
                        }

                        functionDeclaration {
                            typeParameter {
                                name { "$returnPackage.$returnType" }
                            }

                            returnType {
                                unitReturnType()
                            }
                        }
                    }
                }

            }

            returnType {
                unitReturnType()
            }
        }

        val pipelineThreeName = "pipelineThree"
        val pipelineThree = functionDeclaration {
            setupHandler(
                pipelineThreeName, additionalData = PipelineMetadata(
                    mockkClass(TypeName::class), 1,
                    PipelineTarget.BOTH
                )
            )

            parameter {
                name { "request" }

                typeRef {
                    inputFourClass()
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
                                anyReturnType()
                            }
                        }

                        functionDeclaration {

                            typeParameter {
                                name { "$returnPackage.$returnType" }
                            }

                            returnType {
                                anyReturnType()
                            }
                        }
                    }
                }

            }

            returnType {
                anyReturnType()
            }
        }

        val pipelineFourName = "pipelineFour"
        val pipelineFour = functionDeclaration {
            setupHandler(
                pipelineFourName, additionalData = PipelineMetadata(
                    mockkClass(TypeName::class),
                    Int.MIN_VALUE, PipelineTarget.REQUEST_MATCH
                )
            )

            parameter {
                name { "request" }

                typeRef {
                    inputFourClass()
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
                                returnTypePart()
                            }
                        }

                        functionDeclaration {
                            typeParameter {
                                name { "$returnPackage.$returnType" }
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

        val pipelineFiveName = "pipelineFive"
        val pipelineFive = functionDeclaration {
            setupHandler(
                pipelineFiveName, additionalData = PipelineMetadata(
                    mockkClass(TypeName::class),
                    Int.MIN_VALUE, PipelineTarget.NOTIFICATIONS
                )
            )

            parameter {
                name { "request" }

                typeRef {
                    inputTwoClass()
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
                                unitReturnType()
                            }
                        }

                        functionDeclaration {
                            typeParameter {
                                name { "$returnPackage.$returnType" }
                            }

                            returnType {
                                unitReturnType()
                            }
                        }
                    }
                }

            }

            returnType {
                unitReturnType()
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

            NotificationHandlerVisitor().visitFunctionDeclaration(notificationOne, Unit)
            NotificationHandlerVisitor().visitFunctionDeclaration(notificationTwo, Unit)

            PipelineHandlerVisitor().visitFunctionDeclaration(pipelineOne, Unit)
            PipelineHandlerVisitor().visitFunctionDeclaration(pipelineTwo, Unit)
            PipelineHandlerVisitor().visitFunctionDeclaration(pipelineThree, Unit)
            PipelineHandlerVisitor().visitFunctionDeclaration(pipelineFour, Unit)
            PipelineHandlerVisitor().visitFunctionDeclaration(pipelineFive, Unit)

            process(resolver)
        }

        val mediatorCode = generatedCode.drop(2).first()
        assert(mediatorCode.contains(": Mediator"))
        assert(mediatorCode.contains("override suspend fun send"))
        assert(mediatorCode.contains(": suspend () -> Handler__${handlerOneName}__1"))
        assert(mediatorCode.contains(": suspend () -> Handler__${handlerTwoName}__2"))
        assert(mediatorCode.contains(": suspend () -> Handler__${handlerThreeName}__3"))
        assert(mediatorCode.contains(": suspend () -> Handler__${handlerFourName}__4"))
        assert(mediatorCode.contains(": suspend () -> Handler__${notificationOneName}__5"))
        assert(mediatorCode.contains(": suspend () -> Handler__${notificationTwoName}__6"))
        assert(mediatorCode.contains(": suspend () -> Handler__${pipelineOneName}__7"))
        assert(mediatorCode.contains(": suspend () -> Handler__${pipelineTwoName}__8"))
        assert(mediatorCode.contains(": suspend () -> Handler__${pipelineThreeName}__9"))
        assert(mediatorCode.contains(": suspend () -> Handler__${pipelineFourName}__10"))
        assert(mediatorCode.contains(": suspend () -> Handler__${pipelineFiveName}__11"))
        assert(mediatorCode.contains("is $inputOneType ->"))
        assert(mediatorCode.contains("is $inputTwoType ->"))
        assert(mediatorCode.contains("is $inputThreeType ->"))
        assert(mediatorCode.contains("is $inputFourType ->"))

        assert(
            mediatorCode.contains("""is $inputOneType -> {
      handler__${pipelineThreeName.lowercase()}__9().handleRequest(message) {
        handler__${pipelineFourName.lowercase()}__10().handleRequest(message) {
          handler__${pipelineOneName.lowercase()}__7().handleRequest(message) {
            handler__${handlerOneName.lowercase()}__1().handleRequest(message)
          }
        }
      }
    }""".trimMargin()
            )
        )


        assert(
            mediatorCode.contains("""is $inputThreeType -> {
      handler__${pipelineThreeName.lowercase()}__9().handleRequest(message) {
        handler__${pipelineFourName.lowercase()}__10().handleRequest(message) {
          handler__${handlerThreeName.lowercase()}__3().handleRequest(message)
        }
      }
    }""".trimMargin()
            )
        )

        assert(
            mediatorCode.contains(
                """is $inputTwoType -> {
      handler__${pipelineThreeName.lowercase()}__9().handleRequest(message) {
        handler__${pipelineTwoName.lowercase()}__8().handleRequest(message) {
          handler__${handlerTwoName.lowercase()}__2().handleRequest(message)
        }
      }
    }""".trimMargin()
            )
        )

        assert(
            mediatorCode.contains("""is $inputFourType -> {
      handler__${pipelineThreeName.lowercase()}__9().handleRequest(message) {
        handler__${pipelineFourName.lowercase()}__10().handleRequest(message) {
          handler__${handlerFourName.lowercase()}__4().handleRequest(message)
        }
      }
    }""".trimMargin()
            )
        )

        assert(
            mediatorCode.indexOf(
                "is $inputOneType ->"
            ) < mediatorCode.indexOf("is $inputThreeType ->")
        )
        assert(
            mediatorCode.indexOf(
                "is $inputOneType ->"
            ) < mediatorCode.indexOf("is $inputTwoType ->")
        )
        assert(
            mediatorCode.indexOf(
                "is $inputOneType ->"
            ) < mediatorCode.indexOf("is $inputFourType ->")
        )

        assert(
            mediatorCode.indexOf(
                "is $inputTwoType ->"
            ) < mediatorCode.indexOf("is $inputFourType ->")
        )

        assert(
            mediatorCode.indexOf(
                "is $inputThreeType ->"
            ) < mediatorCode.indexOf("is $inputFourType ->")
        )

        assert(
            mediatorCode.contains(
                """is $inputTwoType -> {
        coroutineScope {
          handler__${pipelineFiveName.lowercase()}__11().handleRequest(notification) {
            handler__${pipelineThreeName.lowercase()}__9().handleRequest(notification) {
              launch {
                handler__${notificationOneName.lowercase()}__5().handleRequest(notification)
              }
            }
          }
        }
      }""".trimMargin()
            )
        )

        assert(
            mediatorCode.contains(
                """is $inputFourType -> {
        coroutineScope {
          handler__${pipelineThreeName.lowercase()}__9().handleRequest(notification) {
            handler__${notificationTwoName.lowercase()}__6().handleRequest(notification)
          }
        }
      }""".trimMargin()
            )
        )
    }


    /*
    input:
       InputType1 <: InputType2

       rq1 -> RequestHandler(paramPackage.InputType1) : ReturnType
       rq2 -> RequestHandler(paramPackage.InputType2) : Unit

       nf1 -> NotificationHandler(paramPackage.InputType1)
       nf2 -> NotificationHandler(paramPackage.InputType2)

       pl3 -> PipelineBehavior(paramPackage.InputType2): Any? (REQUESTS)
       pl2 -> PipelineBehaviour(paramPackage.InputType2): Any? (BOTH)
       pl1 -> PipelineBehavior(paramPackage.InputType1): Any? (BOTH)

    output:
       invoke()
           is InputType1 -> pl1 pl2 pl3 rq1
           is InputType2 -> pl2 pl3 rq2

       publish()
           is InputType1 -> pl1 pl2 nf1 nf2
           is InputType2 -> pl2 nf2
    */
    @Test
    fun `multiple pipeline behaviors with strict and pass targets and simple type hierarchy`() {
        val inputPackage = "paramPackage"
        val inputOneType = "InputType1"
        val inputTwoType = "InputType2"

        val returnPackage = "returnPackage"
        val returnType = "ReturnType"

        val inputTwoClass: KSTypeReference.() -> Unit = {
            type {
                nullability { Nullability.NOT_NULL }
                classDeclaration {
                    packageName { inputPackage }
                    qualifiedName { "$inputPackage.$inputTwoType" }
                    classKind { ClassKind.CLASS }
                }
            }
        }

        val inputOneClass: KSTypeReference.() -> Unit = {
            type {
                nullability { Nullability.NOT_NULL }
                classDeclaration {
                    packageName { inputPackage }
                    qualifiedName { "$inputPackage.$inputOneType" }
                    classKind { ClassKind.CLASS }

                    superType {
                        inputTwoClass()
                    }
                }
            }
        }


        // -- handlers

        val returnTypePart: KSTypeReference.() -> Unit = {
            type {
                nullability { Nullability.NOT_NULL }
                classDeclaration {
                    packageName { returnPackage }
                    qualifiedName { "$returnPackage.$returnType" }
                    classKind { ClassKind.CLASS }
                }
            }
        }

        val unitReturnType: KSTypeReference.() -> Unit = {
            type {
                nullability { Nullability.NOT_NULL }
                classDeclaration {
                    packageName { "kotlin" }
                    qualifiedName { "kotlin.Unit" }
                    classKind { ClassKind.OBJECT }
                }
            }
        }

        val handlerOneName = "handlerOne"
        val handlerOne = functionDeclaration {
            setupHandler(handlerOneName)

            parameter {
                typeRef {
                    inputOneClass()
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
                    inputTwoClass()
                }
            }

            returnType {
                unitReturnType()
            }
        }

        // -- notifications

        val notificationOneName = "notificationOne"
        val notificationOne = functionDeclaration {
            setupHandler(
                notificationOneName,
                additionalData = NotificationHandlerMetadata(NotificationParallel.PARALLEL, 2)
            )

            parameter {
                typeRef {
                    inputOneClass()
                }
            }

            returnType {
                unitReturnType()
            }
        }


        val notificationTwoName = "notificationTwo"
        val notificationTwo = functionDeclaration {
            setupHandler(
                notificationTwoName,
                additionalData = NotificationHandlerMetadata(NotificationParallel.SEQUENTIAL, 1)
            )

            parameter {
                typeRef {
                    inputTwoClass()
                }
            }

            returnType {
                unitReturnType()
            }
        }

        // -- pipelines

        val anyReturnType: KSTypeReference.() -> Unit = {
            type {
                nullability { Nullability.NULLABLE }
                classDeclaration {
                    packageName { "kotlin" }
                    qualifiedName { "kotlin.Any" }
                    classKind { ClassKind.CLASS }
                }
            }
        }

        val pipelineOneName = "pipelineOne"
        val pipelineOne = functionDeclaration {
            setupHandler(
                pipelineOneName,
                additionalData = PipelineMetadata(mockkClass(TypeName::class), 0,
                    PipelineTarget.BOTH)
            )

            parameter {
                name { "request" }

                typeRef {
                    inputOneClass()
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
                                anyReturnType()
                            }
                        }

                        functionDeclaration {
                            typeParameter {
                                name { "kotlin.Any" }
                            }

                            returnType {
                                anyReturnType()
                            }
                        }
                    }
                }

            }

            returnType {
                anyReturnType()
            }
        }

        val pipelineTwoName = "pipelineTwo"
        val pipelineTwo = functionDeclaration {
            setupHandler(
                pipelineTwoName,
                additionalData = PipelineMetadata(mockkClass(TypeName::class), 2,
                    PipelineTarget.BOTH)
            )

            parameter {
                name { "request" }

                typeRef {
                    inputTwoClass()
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
                                anyReturnType()
                            }
                        }

                        functionDeclaration {
                            typeParameter {
                                name { "$returnPackage.$returnType" }
                            }

                            returnType {
                                anyReturnType()
                            }
                        }
                    }
                }

            }

            returnType {
                anyReturnType()
            }
        }

        val pipelineThreeName = "pipelineThree"
        val pipelineThree = functionDeclaration {
            setupHandler(
                pipelineThreeName, additionalData = PipelineMetadata(
                    mockkClass(TypeName::class), 1,
                    PipelineTarget.REQUESTS
                )
            )

            parameter {
                name { "request" }

                typeRef {
                    inputTwoClass()
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
                                anyReturnType()
                            }
                        }

                        functionDeclaration {
                            typeParameter {
                                name { "kotlin.Any" }
                            }

                            returnType {
                                anyReturnType()
                            }
                        }
                    }
                }

            }

            returnType {
                anyReturnType()
            }
        }

        val generatedCode = generateStringOutput(1) {
            val resolver = mockkClass(Resolver::class)
            every { resolver.getSymbolsWithAnnotation("annotations.RequestHandler") } returns sequenceOf()
            every { resolver.getSymbolsWithAnnotation("annotations.NotificationHandler") } returns sequenceOf()
            every { resolver.getSymbolsWithAnnotation("annotations.PipelineBehavior") } returns sequenceOf()

            RequestHandlerVisitor().visitFunctionDeclaration(handlerOne, Unit)
            RequestHandlerVisitor().visitFunctionDeclaration(handlerTwo, Unit)

            NotificationHandlerVisitor().visitFunctionDeclaration(notificationOne, Unit)
            NotificationHandlerVisitor().visitFunctionDeclaration(notificationTwo, Unit)

            PipelineHandlerVisitor().visitFunctionDeclaration(pipelineOne, Unit)
            PipelineHandlerVisitor().visitFunctionDeclaration(pipelineTwo, Unit)
            PipelineHandlerVisitor().visitFunctionDeclaration(pipelineThree, Unit)

            process(resolver)
        }

        val mediatorCode = generatedCode.drop(2).first()
        assert(mediatorCode.contains(": Mediator"))
        assert(mediatorCode.contains("override suspend fun send"))
        assert(mediatorCode.contains(": suspend () -> Handler__${handlerOneName}__1"))
        assert(mediatorCode.contains(": suspend () -> Handler__${handlerTwoName}__2"))
        assert(mediatorCode.contains(": suspend () -> Handler__${notificationOneName}__3"))
        assert(mediatorCode.contains(": suspend () -> Handler__${notificationTwoName}__4"))
        assert(mediatorCode.contains(": suspend () -> Handler__${pipelineOneName}__5"))
        assert(mediatorCode.contains(": suspend () -> Handler__${pipelineTwoName}__6"))
        assert(mediatorCode.contains(": suspend () -> Handler__${pipelineThreeName}__7"))
        assert(mediatorCode.contains("is $inputOneType ->"))
        assert(mediatorCode.contains("is $inputTwoType ->"))

        assert(
            mediatorCode.contains("""is $inputOneType -> {
      handler__${pipelineOneName.lowercase()}__5().handleRequest(message) {
        handler__${pipelineThreeName.lowercase()}__7().handleRequest(message) {
          handler__${pipelineTwoName.lowercase()}__6().handleRequest(message) {
            handler__${handlerOneName.lowercase()}__1().handleRequest(message)
          }
        }
      }
    }""".trimMargin()
            )
        )

        assert(
            mediatorCode.contains(
                """is $inputTwoType -> {
      handler__${pipelineThreeName.lowercase()}__7().handleRequest(message) {
        handler__${pipelineTwoName.lowercase()}__6().handleRequest(message) {
          handler__${handlerTwoName.lowercase()}__2().handleRequest(message)
        }
      }
    }""".trimMargin()
            )
        )

        assert(
            mediatorCode.indexOf(
                "is -> $inputOneType"
            ) < mediatorCode.indexOf("is $inputTwoType ->")
        )


        assert(
            mediatorCode.contains(
                """is $inputOneType -> {
        coroutineScope {
          handler__${pipelineOneName.lowercase()}__5().handleRequest(notification) {
            handler__${pipelineTwoName.lowercase()}__6().handleRequest(notification) {
              launch {
                handler__${notificationOneName.lowercase()}__3().handleRequest(notification)
              }
            }
          }
        }
      }""".trimMargin()
            )
        )

        assert(
            mediatorCode.contains(
                """is $inputTwoType -> {
        coroutineScope {
          handler__${pipelineTwoName.lowercase()}__6().handleRequest(notification) {
            handler__${notificationTwoName.lowercase()}__4().handleRequest(notification)
          }
        }
      }""".trimMargin()
            )
        )
    }
}
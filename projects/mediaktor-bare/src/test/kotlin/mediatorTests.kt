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
        val packageName = "handlers"

        val handlerOne = functionDeclaration {
            setupHandler(handlerOneName, packageName)

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
            setupHandler(handlerTwoName, packageName)

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

        val generatedCode = generateStringOutput(2) {
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
        assert(mediatorCode.contains(": suspend () -> Handler__${packageName}__${handlerOneName}"))
        assert(mediatorCode.contains(": suspend () -> Handler__${packageName}__${handlerTwoName}"))
        assert(mediatorCode.contains("is $inputOneClass ->"))
        assert(mediatorCode.contains("is $inputTwoClass ->"))
    }

    @Test
    fun `multiple notifiers with same input type test`() {
        val inputOneClass = "TestInputClass1"
        val handlerOneName = "testHandler1"
        val packageName = "handlers"

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
                packageName,
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
                packageName,
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

        val generatedCode = generateStringOutput(2) {
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
        assert(mediatorCode.contains(": suspend () -> Handler__${packageName}__${handlerOneName}"))
        assert(mediatorCode.contains(": suspend () -> Handler__${packageName}__${handlerTwoName}"))
        assert(mediatorCode.contains("is $inputOneClass ->"))
        assert(
            mediatorCode.contains(
                """is $inputOneClass -> {
        coroutineScope {
          handler__${packageName.lowercase()}__${handlerOneName.lowercase()}().handleRequest(notification)
          handler__${packageName.lowercase()}__${handlerTwoName.lowercase()}().handleRequest(notification)
        }
      }""".trimMargin()
            )
        )
    }

    @Test
    fun `multiple notifiers with different request types test`() {
        val inputOneClass = "TestInputClass1"
        val handlerOneName = "testHandler1"
        val packageName = "handlers"

        val handlerOne = functionDeclaration {
            setupHandlerReturn(
                handlerOneName,
                packageName,
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
                packageName,
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

        val generatedCode = generateStringOutput(2) {
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
        assert(mediatorCode.contains(": suspend () -> Handler__${packageName}__${handlerOneName}"))
        assert(mediatorCode.contains(": suspend () -> Handler__${packageName}__${handlerTwoName}"))
        assert(mediatorCode.contains("is $inputOneClass ->"))
        assert(mediatorCode.contains("is $inputTwoClass ->"))
        assert(mediatorCode.contains("handler__${packageName.lowercase()}__${handlerOneName.lowercase()}().handleRequest(notification)"))
        assert(mediatorCode.contains("handler__${packageName.lowercase()}__${handlerTwoName.lowercase()}().handleRequest(notification)"))
        assert(
            !mediatorCode.contains(
                """handler__${packageName.lowercase()}__${handlerOneName.lowercase()}().handleRequest(notification)
            |          handler__${packageName.lowercase()}__${handlerTwoName.lowercase()}().handleRequest(notification)
        """.trimMargin()
            )
        )
    }

    @Test
    fun `multiple parallel notifiers with reversed order`() {
        val inputOneClass = "TestInputClass1"
        val handlerOneName = "testHandler1"
        val packageName = "handlers"

        val handlerOne = functionDeclaration {
            setupHandlerReturn(
                handlerOneName,
                packageName,
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
                packageName,
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

        val generatedCode = generateStringOutput(2) {
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
        assert(mediatorCode.contains(": suspend () -> Handler__${packageName}__${handlerOneName}"))
        assert(mediatorCode.contains(": suspend () -> Handler__${packageName}__${handlerTwoName}"))
        assert(mediatorCode.contains("is $inputOneClass ->"))
        assert(mediatorCode.contains("is $inputTwoClass ->"))

        assert(
            mediatorCode.indexOf(
                "${handlerOneName.lowercase()}()"
            ) > mediatorCode.indexOf("${handlerTwoName.lowercase()}()")
        )

        assert(
            mediatorCode.contains(
                """launch {
                |            handler__${packageName.lowercase()}__${handlerOneName.lowercase()}().handleRequest(notification)
                |          }
        """.trimMargin()
            )
        )
        assert(
            mediatorCode.contains(
                """launch {
                |            handler__${packageName.lowercase()}__${handlerTwoName.lowercase()}().handleRequest(notification)
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

        val packageName = "handlers"

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
            setupHandler(handlerOneName, packageName)

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
            setupHandler(handlerTwoName, packageName)

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
            setupHandler(handlerThreeName, packageName)

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
            setupHandler(handlerFourName, packageName)

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
                packageName,
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
                packageName,
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
                packageName,
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
                packageName,
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

        val generatedCode = generateStringOutput(8) {
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

        val mediatorCode = generatedCode.drop(8).first()
        assert(mediatorCode.contains(": Mediator"))
        assert(mediatorCode.contains("override suspend fun send"))
        assert(mediatorCode.contains(": suspend () -> Handler__${packageName}__${handlerOneName}"))
        assert(mediatorCode.contains(": suspend () -> Handler__${packageName}__${handlerTwoName}"))
        assert(mediatorCode.contains(": suspend () -> Handler__${packageName}__${handlerThreeName}"))
        assert(mediatorCode.contains(": suspend () -> Handler__${packageName}__${handlerFourName}"))
        assert(mediatorCode.contains(": suspend () -> Handler__${packageName}__${pipelineOneName}"))
        assert(mediatorCode.contains(": suspend () -> Handler__${packageName}__${pipelineTwoName}"))
        assert(mediatorCode.contains(": suspend () -> Handler__${packageName}__${pipelineThreeName}"))
        assert(mediatorCode.contains(": suspend () -> Handler__${packageName}__${pipelineFourName}"))
        assert(mediatorCode.contains("is $inputOneType ->"))
        assert(mediatorCode.contains("is $inputTwoType ->"))
        assert(mediatorCode.contains("is $inputThreeType ->"))
        assert(mediatorCode.contains("is $inputFourType ->"))

        assert(
            mediatorCode.contains(
                """is $inputOneType -> {
      handler__${packageName.lowercase()}__${pipelineFourName.lowercase()}().handleRequest(message) {
        handler__${packageName.lowercase()}__${pipelineOneName.lowercase()}().handleRequest(message) {
          handler__${packageName.lowercase()}__${pipelineThreeName.lowercase()}().handleRequest(message) {
            handler__${packageName.lowercase()}__${pipelineTwoName.lowercase()}().handleRequest(message) {
              handler__${packageName.lowercase()}__${handlerOneName.lowercase()}().handleRequest(message)
            }""".trimMargin()
            )
        )

        assert(
            mediatorCode.contains(
                """is $inputThreeType -> {
      handler__${packageName.lowercase()}__${pipelineFourName.lowercase()}().handleRequest(message) {
        handler__${packageName.lowercase()}__${pipelineThreeName.lowercase()}().handleRequest(message) {
          handler__${packageName.lowercase()}__${handlerThreeName.lowercase()}().handleRequest(message)
        }
      }
    }""".trimMargin()
            )
        )

        assert(
            mediatorCode.contains(
                """is $inputTwoType -> {
      handler__${packageName.lowercase()}__${pipelineFourName.lowercase()}().handleRequest(message) {
        handler__${packageName.lowercase()}__${pipelineTwoName.lowercase()}().handleRequest(message) {
          handler__${packageName.lowercase()}__${handlerTwoName.lowercase()}().handleRequest(message)
        }
      }
    }""".trimMargin()
            )
        )

        assert(
            mediatorCode.contains(
                """is $inputFourType -> {
      handler__${packageName.lowercase()}__${pipelineFourName.lowercase()}().handleRequest(message) {
        handler__${packageName.lowercase()}__${handlerFourName.lowercase()}().handleRequest(message)
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

        val packageName = "handlers"

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
            setupHandler(handlerOneName, packageName)

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
            setupHandler(handlerTwoName, packageName)

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
            setupHandler(handlerThreeName, packageName)

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
            setupHandler(handlerFourName, packageName)

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
                packageName,
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
                packageName,
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
                packageName,
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
                packageName,
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
                pipelineThreeName,
                packageName,
                additionalData = PipelineMetadata(
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
                pipelineFourName, 
                packageName,
                additionalData = PipelineMetadata(
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
                pipelineFiveName,
                packageName,
                additionalData = PipelineMetadata(
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

        val generatedCode = generateStringOutput(11) {
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

        val mediatorCode = generatedCode.drop(11).first()
        assert(mediatorCode.contains(": Mediator"))
        assert(mediatorCode.contains("override suspend fun send"))
        assert(mediatorCode.contains(": suspend () -> Handler__${packageName}__${handlerOneName}"))
        assert(mediatorCode.contains(": suspend () -> Handler__${packageName}__${handlerTwoName}"))
        assert(mediatorCode.contains(": suspend () -> Handler__${packageName}__${handlerThreeName}"))
        assert(mediatorCode.contains(": suspend () -> Handler__${packageName}__${handlerFourName}"))
        assert(mediatorCode.contains(": suspend () -> Handler__${packageName}__${notificationOneName}"))
        assert(mediatorCode.contains(": suspend () -> Handler__${packageName}__${notificationTwoName}"))
        assert(mediatorCode.contains(": suspend () -> Handler__${packageName}__${pipelineOneName}"))
        assert(mediatorCode.contains(": suspend () -> Handler__${packageName}__${pipelineTwoName}"))
        assert(mediatorCode.contains(": suspend () -> Handler__${packageName}__${pipelineThreeName}"))
        assert(mediatorCode.contains(": suspend () -> Handler__${packageName}__${pipelineFourName}"))
        assert(mediatorCode.contains(": suspend () -> Handler__${packageName}__${pipelineFiveName}"))
        assert(mediatorCode.contains("is $inputOneType ->"))
        assert(mediatorCode.contains("is $inputTwoType ->"))
        assert(mediatorCode.contains("is $inputThreeType ->"))
        assert(mediatorCode.contains("is $inputFourType ->"))

        assert(
            mediatorCode.contains("""is $inputOneType -> {
      handler__${packageName.lowercase()}__${pipelineThreeName.lowercase()}().handleRequest(message) {
        handler__${packageName.lowercase()}__${pipelineFourName.lowercase()}().handleRequest(message) {
          handler__${packageName.lowercase()}__${pipelineOneName.lowercase()}().handleRequest(message) {
            handler__${packageName.lowercase()}__${handlerOneName.lowercase()}().handleRequest(message)
          }
        }
      }
    }""".trimMargin()
            )
        )


        assert(
            mediatorCode.contains("""is $inputThreeType -> {
      handler__${packageName.lowercase()}__${pipelineThreeName.lowercase()}().handleRequest(message) {
        handler__${packageName.lowercase()}__${pipelineFourName.lowercase()}().handleRequest(message) {
          handler__${packageName.lowercase()}__${handlerThreeName.lowercase()}().handleRequest(message)
        }
      }
    }""".trimMargin()
            )
        )

        assert(
            mediatorCode.contains(
                """is $inputTwoType -> {
      handler__${packageName.lowercase()}__${pipelineThreeName.lowercase()}().handleRequest(message) {
        handler__${packageName.lowercase()}__${pipelineTwoName.lowercase()}().handleRequest(message) {
          handler__${packageName.lowercase()}__${handlerTwoName.lowercase()}().handleRequest(message)
        }
      }
    }""".trimMargin()
            )
        )

        assert(
            mediatorCode.contains("""is $inputFourType -> {
      handler__${packageName.lowercase()}__${pipelineThreeName.lowercase()}().handleRequest(message) {
        handler__${packageName.lowercase()}__${pipelineFourName.lowercase()}().handleRequest(message) {
          handler__${packageName.lowercase()}__${handlerFourName.lowercase()}().handleRequest(message)
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
          handler__${packageName.lowercase()}__${pipelineFiveName.lowercase()}().handleRequest(notification) {
            handler__${packageName.lowercase()}__${pipelineThreeName.lowercase()}().handleRequest(notification) {
              launch {
                handler__${packageName.lowercase()}__${notificationOneName.lowercase()}().handleRequest(notification)
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
          handler__${packageName.lowercase()}__${pipelineThreeName.lowercase()}().handleRequest(notification) {
            handler__${packageName.lowercase()}__${notificationTwoName.lowercase()}().handleRequest(notification)
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

        val packageName = "handlers"

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
            setupHandler(handlerOneName, packageName)

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
            setupHandler(handlerTwoName, packageName)

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
                packageName,
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
                packageName,
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
                packageName,
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
                packageName,
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
                pipelineThreeName, 
                packageName,
                additionalData = PipelineMetadata(
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

        val generatedCode = generateStringOutput(7) {
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

        val mediatorCode = generatedCode.drop(7).first()
        assert(mediatorCode.contains(": Mediator"))
        assert(mediatorCode.contains("override suspend fun send"))
        assert(mediatorCode.contains(": suspend () -> Handler__${packageName}__${handlerOneName}"))
        assert(mediatorCode.contains(": suspend () -> Handler__${packageName}__${handlerTwoName}"))
        assert(mediatorCode.contains(": suspend () -> Handler__${packageName}__${notificationOneName}"))
        assert(mediatorCode.contains(": suspend () -> Handler__${packageName}__${notificationTwoName}"))
        assert(mediatorCode.contains(": suspend () -> Handler__${packageName}__${pipelineOneName}"))
        assert(mediatorCode.contains(": suspend () -> Handler__${packageName}__${pipelineTwoName}"))
        assert(mediatorCode.contains(": suspend () -> Handler__${packageName}__${pipelineThreeName}"))
        assert(mediatorCode.contains("is $inputOneType ->"))
        assert(mediatorCode.contains("is $inputTwoType ->"))

        assert(
            mediatorCode.contains("""is $inputOneType -> {
      handler__${packageName.lowercase()}__${pipelineOneName.lowercase()}().handleRequest(message) {
        handler__${packageName.lowercase()}__${pipelineThreeName.lowercase()}().handleRequest(message) {
          handler__${packageName.lowercase()}__${pipelineTwoName.lowercase()}().handleRequest(message) {
            handler__${packageName.lowercase()}__${handlerOneName.lowercase()}().handleRequest(message)
          }
        }
      }
    }""".trimMargin()
            )
        )

        assert(
            mediatorCode.contains(
                """is $inputTwoType -> {
      handler__${packageName.lowercase()}__${pipelineThreeName.lowercase()}().handleRequest(message) {
        handler__${packageName.lowercase()}__${pipelineTwoName.lowercase()}().handleRequest(message) {
          handler__${packageName.lowercase()}__${handlerTwoName.lowercase()}().handleRequest(message)
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
          handler__${packageName.lowercase()}__${pipelineOneName.lowercase()}().handleRequest(notification) {
            handler__${packageName.lowercase()}__${pipelineTwoName.lowercase()}().handleRequest(notification) {
              launch {
                handler__${packageName.lowercase()}__${notificationOneName.lowercase()}().handleRequest(notification)
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
          handler__${packageName.lowercase()}__${pipelineTwoName.lowercase()}().handleRequest(notification) {
            handler__${packageName.lowercase()}__${notificationTwoName.lowercase()}().handleRequest(notification)
          }
        }
      }""".trimMargin()
            )
        )
    }
}
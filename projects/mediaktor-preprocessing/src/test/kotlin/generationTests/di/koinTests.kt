package generationTests.di

import annotations.HandlerLifespan
import com.google.devtools.ksp.processing.Resolver
import functionDeclaration
import generateStringOutput
import io.mockk.every
import io.mockk.mockkClass
import setupHandlerLifespan
import kotlin.test.Test

class KoinTests {
    @Test
    fun `multiple handlers test`() {
        val handlerOneName = "HandlerOne"
        val handlerTwoName = "HandlerTwo"

        val funOneDecl = functionDeclaration {
            setupHandlerLifespan(handlerOneName, HandlerLifespan.SINGLE)
        }

        val funTwoDecl = functionDeclaration {
            setupHandlerLifespan(handlerTwoName, HandlerLifespan.FACTORY)
        }

        val generatedCode = generateStringOutput(1) {
            val resolver = mockkClass(Resolver::class)
            every { resolver.getSymbolsWithAnnotation("annotations.RequestHandler") } returns sequenceOf()
            every { resolver.getSymbolsWithAnnotation("annotations.NotificationHandler") } returns sequenceOf()
            every { resolver.getSymbolsWithAnnotation("annotations.PipelineBehavior") } returns sequenceOf()

            RequestHandlerVisitor().visitFunctionDeclaration(funOneDecl, Unit)
            RequestHandlerVisitor().visitFunctionDeclaration(funTwoDecl, Unit)
            process(resolver)
        }

        val mediatorClassName = "Mediator__Impl"
        val mediatorInterface = "interfaces.Mediator"
        val handlerCode = generatedCode.last()
        assert(handlerCode.contains("package mediaktorKoin"))
        assert(handlerCode.contains("fun Module.provideMediator()"))
        assert(handlerCode.contains("singleOf(::Handler__${handlerOneName}__1)"))
        assert(handlerCode.contains("factoryOf(::Handler__${handlerTwoName}__2)"))
        assert(handlerCode.contains("single { params -> $mediatorClassName"))
        assert(handlerCode.contains("bind $mediatorInterface::class"))
    }
}
import mediaktor.core.annotations.HandlerLifespan
import com.google.devtools.ksp.processing.Resolver
import testUtils.functionDeclaration
import testUtils.generateStringOutput
import io.mockk.every
import io.mockk.mockkClass
import preprocessing.KoinProcessor
import testUtils.setupHandlerLifespan
import kotlin.math.log
import kotlin.test.Test

class KoinTests {
    @Test
    fun `multiple handlers test`() {
        val handlerOneName = "HandlerOne"
        val handlerTwoName = "HandlerTwo"
        val packageName = "handlers"

        val funOneDecl = functionDeclaration {
            setupHandlerLifespan(handlerOneName, packageName,HandlerLifespan.SINGLE)
        }

        val funTwoDecl = functionDeclaration {
            setupHandlerLifespan(handlerTwoName, packageName, HandlerLifespan.FACTORY)
        }

        val generatedCode = generateStringOutput(1, { codeGenerator, logger -> KoinProcessor(codeGenerator, logger) }) {
            val resolver = mockkClass(Resolver::class)
            every { resolver.getSymbolsWithAnnotation("mediaktor.core.annotations.RequestHandler") } returns sequenceOf()
            every { resolver.getSymbolsWithAnnotation("mediaktor.core.annotations.NotificationHandler") } returns sequenceOf()
            every { resolver.getSymbolsWithAnnotation("mediaktor.core.annotations.PipelineBehavior") } returns sequenceOf()

            RequestHandlerVisitor().visitFunctionDeclaration(funOneDecl, Unit)
            RequestHandlerVisitor().visitFunctionDeclaration(funTwoDecl, Unit)
            process(resolver)
        }

        val mediatorClassName = "Mediator__Impl"
        val mediatorInterface = "mediaktor.core.interfaces.Mediator"
        val diCode = generatedCode.last()
        assert(diCode.contains("package mediaktor.koin"))
        assert(diCode.contains("fun Module.provideMediator()"))
        assert(diCode.contains("singleOf(::Handler__${packageName}__${handlerOneName})"))
        assert(diCode.contains("factoryOf(::Handler__${packageName}__${handlerTwoName})"))
        assert(diCode.contains("single { params -> $mediatorClassName"))
        assert(diCode.contains("bind $mediatorInterface::class"))
    }
}
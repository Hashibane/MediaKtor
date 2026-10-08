import mediaktor.core.annotations.NotificationHandler
import mediaktor.core.annotations.PipelineBehavior
import mediaktor.core.annotations.RequestHandler
import mediaktor.core.interfaces.Mediator
import kotlinx.coroutines.runBlocking
import mediaktor.koin.provideMediator
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test

data class Request(val content: String)
class Logger

@RequestHandler
fun request(arg: Request, logger: Logger) {}

@NotificationHandler
fun notification(arg: Request, logger: Logger) {}

@PipelineBehavior
suspend fun pipeline(arg: Request, next: suspend () -> Any?, logger: Logger): Any? {
    return next()
}

class IntegrationTests {
    @Test
    fun `multiple handlers arguments test`() {
        val appModule = module {
            singleOf(::Logger)

            provideMediator()
        }

        val koin = koinApplication {
            modules(appModule)
        }.koin

        val mediator: Mediator = koin.get()

        runBlocking {
            mediator.send(Request("1"))
            mediator.publish(Request("2"))
        }
    }
}
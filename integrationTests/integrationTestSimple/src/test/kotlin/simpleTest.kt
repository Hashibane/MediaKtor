import Mediator__Impl.Mediator__Impl
import annotations.RequestHandler
import kotlinx.coroutines.runBlocking
import kotlin.test.Test

@RequestHandler
fun testHandler(arg: Int): String = arg.toString()

class IntegrationTests {
    @Test
    fun `simple integration test`() {
        val mediator = Mediator__Impl({ Handler____testHandler() })

        runBlocking {
            assert(mediator.send(4) == "4")
        }
    }
}
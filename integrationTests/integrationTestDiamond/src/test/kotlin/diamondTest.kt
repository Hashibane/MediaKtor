import mediaktor.core.annotations.NotificationHandler
import mediaktor.core.annotations.NotificationParallel
import mediaktor.core.annotations.PipelineBehavior
import mediaktor.core.annotations.PipelineTarget
import mediaktor.core.annotations.RequestHandler
import mediaktor.core.interfaces.Mediator
import kotlinx.coroutines.runBlocking
import mediaktor.koin.provideMediator
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test


interface Type4 {
    val content: String
}

interface Type3: Type4

interface Type2: Type4

data class Type1(override val content: String) : Type3, Type2

val eventQueue = ArrayDeque<String>()

@RequestHandler
fun requestOne(arg: Type1): Type1 {
    eventQueue.add("requestOne")
    return arg
}

@NotificationHandler(parallel = NotificationParallel.SEQUENTIAL)
fun notificationOne(arg: Type1): Unit {
    eventQueue.add("notificationOne")
}

@NotificationHandler(parallel = NotificationParallel.SEQUENTIAL)
fun notificationTwo(arg: Type2): Unit {
    eventQueue.add("notificationTwo")
}

@NotificationHandler(parallel = NotificationParallel.SEQUENTIAL)
fun notificationThree(arg: Type3): Unit {
    eventQueue.add("notificationThree")
}

@NotificationHandler(parallel = NotificationParallel.SEQUENTIAL)
fun notificationFour(arg: Type4): Unit {
    eventQueue.add("notificationFour")
}

@PipelineBehavior
suspend fun pipelineOne(arg: Any, next: suspend () -> Any?): Any? {
    eventQueue.add("pipelineOne")
    return next()
}

@PipelineBehavior(order = 4,target = PipelineTarget.NOTIFICATIONS)
suspend fun pipelineTwo(arg: Type1, next: suspend () -> Unit) {
    eventQueue.add("pipelineTwo")
    next()
}

@PipelineBehavior(order = 3, target = PipelineTarget.NOTIFICATIONS)
suspend fun pipelineThree(arg: Type2, next: suspend () -> Unit) {
    eventQueue.add("pipelineThree")
    next()
}

@PipelineBehavior(order = 2, target = PipelineTarget.NOTIFICATIONS)
suspend fun pipelineFour(arg: Type3, next: suspend () -> Unit) {
    eventQueue.add("pipelineFour")
    next()
}

@PipelineBehavior(order = 1, target = PipelineTarget.NOTIFICATIONS)
suspend fun pipelineFive(arg: Type4, next: suspend () -> Unit) {
    eventQueue.add("pipelineFive")
    next()
}


class IntegrationTests {
    @Test
    fun `diamond integration test`() {
        val appModule = module {
            provideMediator()
        }

        val koin = koinApplication {
            modules(appModule)
        }.koin

        val mediator: Mediator = koin.get()

        val type1Message = Type1("test1")
        runBlocking {
            assert(type1Message == mediator.send(type1Message))
            assert(eventQueue.first() == "pipelineOne")
            assert(eventQueue.drop(1).first() == "requestOne")
            eventQueue.clear()

            mediator.publish(type1Message)
            assert(eventQueue.first() == "pipelineOne")
            assert(eventQueue.drop(1).first() == "pipelineFour")
            assert(eventQueue.drop(2).first() == "pipelineThree")
            assert(eventQueue.drop(3).first() == "pipelineTwo")
            assert(eventQueue.drop(4).first() == "notificationOne")
            eventQueue.clear()

            mediator.publish(object : Type4 {
                override val content: String
                    get() = "four"
            })

            assert(eventQueue.first() == "pipelineOne")
            assert(eventQueue.drop(1).first() == "pipelineFive")
            assert(eventQueue.drop(2).first() == "notificationFour")
            eventQueue.clear()

            mediator.publish(object : Type3 {
                override val content: String
                    get() = "three"
            })

            assert(eventQueue.first() == "pipelineOne")
            assert(eventQueue.drop(1).first() == "pipelineFive")
            assert(eventQueue.drop(2).first() == "pipelineFour")
            assert(eventQueue.drop(3).first() == "notificationThree")
            eventQueue.clear()
        }
    }
}
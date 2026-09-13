package handlers

import annotations.HandlerLifespan
import annotations.NotificationHandler
import annotations.PipelineBehavior
import annotations.PipelineTarget
import annotations.RequestHandler

data class Request(val content: String)


@RequestHandler(HandlerLifespan.SINGLE)
fun testHandler(arg: Request): Unit {

}

@NotificationHandler()
fun testNotifier(arg: Request) {
    println("got the notif 1")
}


@NotificationHandler
fun testNotifierTwo(arg: Request) {
    println("got the notif 2")
}

 
@PipelineBehavior(order = 5, target = PipelineTarget.STRICT_NOTIFICATIONS)
fun verify(arg: Request, next: suspend () -> Unit) {

}

@PipelineBehavior(target = PipelineTarget.PASS_BOTH)
suspend fun verify2(arg: Request, next: suspend () -> Any?): Any? {
    return next()
}
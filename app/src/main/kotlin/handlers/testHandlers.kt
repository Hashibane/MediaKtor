package handlers

import annotations.HandlerLifespan
import annotations.NotificationHandler
import annotations.RequestHandler
import kotlinx.coroutines.coroutineScope

data class Request(val content: String)

@RequestHandler(HandlerLifespan.SINGLE)
fun testHandler(arg: Request?): Unit {

}

@NotificationHandler()
fun testNotifier(arg: Request) {
    println("got the notif 1")
}


@NotificationHandler
fun testNotifierTwo(arg: Request) {
    println("got the notif 2")
}
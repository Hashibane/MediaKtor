package handlers

import annotations.HandlerLifespan
import annotations.RequestHandler

data class Request(val content: String)

@RequestHandler(HandlerLifespan.FACTORY)
fun testHandler(arg: Request?): String? {
    return "Hello $arg!"
}
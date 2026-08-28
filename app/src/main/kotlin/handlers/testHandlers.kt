package handlers

import annotations.HandlerLifespan
import annotations.RequestHandler

@RequestHandler(HandlerLifespan.FACTORY)
fun testHandler(somearg: String): String {
    return "Hello $somearg!"
}
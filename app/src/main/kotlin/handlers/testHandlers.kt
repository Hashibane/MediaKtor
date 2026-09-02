package handlers

import annotations.HandlerLifespan
import annotations.RequestHandler

data class Request(val content: String)
data class Result<T>(val answer: T)

@RequestHandler(HandlerLifespan.FACTORY)
fun testHandler(arg: () -> Request?): Result<String> {
    return Result("Hello $arg!")
}
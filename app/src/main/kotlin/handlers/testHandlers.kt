package handlers

import annotations.RequestHandler

@RequestHandler
fun testHandler(somearg: String): Int {
    return 1
}
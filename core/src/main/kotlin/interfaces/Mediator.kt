package interfaces

interface Mediator {
    suspend fun <T : Any> invoke(request: T): Any?
}

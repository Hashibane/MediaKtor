package interfaces

interface Mediator {
    suspend operator fun <T : Any> invoke(request: T): Any?
    suspend fun <T : Any> publish(notification: T)
}

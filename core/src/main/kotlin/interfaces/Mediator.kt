package interfaces

interface Mediator {
    suspend fun send(message: Any?): Any?
    suspend fun publish(notification: Any?)
}

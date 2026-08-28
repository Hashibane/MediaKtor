package interfaces

interface Mediator {
    suspend operator fun <T : Any> invoke(request: T): Any?
}

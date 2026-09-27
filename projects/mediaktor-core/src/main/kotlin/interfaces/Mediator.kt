package interfaces

/**
 * Mediator interface for use in DI frameworks. The actual implementation is generated
 * by [preprocessing.processors.HandlerProcessor].
 *
 * Use [send] for sending requests and [publish] for notifications.
 */
interface Mediator {
    /** Sends a request via [Mediator] */
    suspend fun send(message: Any?): Any?
    /** Sends a notification via [Mediator] */
    suspend fun publish(notification: Any?)
}

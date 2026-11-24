package mediaktor.core.annotations

/** Determines how many times handler is constructed.
 *
 * [SINGLE] handlers are created once for all calls.
 *
 * [FACTORY] handlers have new instance created for each call.
 */
enum class HandlerLifespan {
    SINGLE,
    FACTORY
}

/** Determines if notification handler gets called wrapped in `launch {}` block.
 *
 * [PARALLEL] means wrapping, [SEQUENTIAL] means no wrapping.
 * */
enum class NotificationParallel {
    SEQUENTIAL,
    PARALLEL
}


/** Specifies to which handlers the pipeline should be applied.
 *
 * Possible values are [REQUEST_MATCH], [REQUESTS], [NOTIFICATIONS] and [BOTH].
 */
enum class PipelineTarget(val isStrict: Boolean) {
    /** [REQUEST_MATCH] pipelines are applied to request handlers with matching request and return type, where the
     * request type and its subtypes are matched. Return type of the pipeline must be the same
     * as return type of the request handler - `T`.
     * The `next` parameter should be of type `suspend () -> T`.
     *
     * If a [REQUEST_MATCH] pipeline does not match the return type but matches the request type,
     * it is not applied and a warning is emitted.
     */
    REQUEST_MATCH(true),

    /**
     * [REQUESTS] pipelines are applied to request handlers matching the request type and its subtypes.
     * Return type of the pipeline should be `Any?` and the `next` parameter type should be `suspend () -> Any?`.
     */
    REQUESTS(false),

    /**
     * [NOTIFICATIONS] pipelines are applied to notification handlers matching the request type and its subtypes.
     * Return type of the pipeline should be `Unit` and the `next` parameter type should be `suspend () -> Unit`.
     */
    NOTIFICATIONS(false),

    /**
     * [BOTH] pipelines are applied to request and notification handlers matching the request type and its subtypes.
     * Return type of the pipeline should be `Any?` and the `next` parameter type should be `suspend () -> Any?`.
     * For notification handlers, the returned value is not passed to next pipelines or [mediaktor.core.interfaces.Mediator.publish] method.
     */
    BOTH(false)
}
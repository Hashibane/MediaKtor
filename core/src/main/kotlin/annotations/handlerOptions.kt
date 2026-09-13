package annotations

enum class HandlerLifespan {
    SINGLE,
    FACTORY
}

enum class NotificationParallel {
    SEQUENTIAL,
    PARALLEL
}


/* Pipeline matching target, which specifies return type and handlers that the pipeline is applied to.
 * STRICT targets match the return type exactly, while PASS must return Any? and are supposed to pass the return of
 * next() whatever the underlying return type is.
 * The BOTH target specifies that the pipeline can be applied to both notification and request handlers.
 * For STRICT matching this means, that it will apply to request handlers that return Unit type and to corresponding
 * notification handlers.
 * PASS pipelines are always applied after the STRICT pipelines according to order value.
 */
enum class PipelineTarget(val isStrict: Boolean) {
    STRICT_REQUESTS(true),
    STRICT_NOTIFICATIONS(true),
    STRICT_BOTH(true),
    PASS_REQUESTS(false),
    PASS_NOTIFICATIONS(false),
    PASS_BOTH(false)
}
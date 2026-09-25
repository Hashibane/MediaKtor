package annotations

/**
 * Request handlers are called on [interfaces.Mediator.send].
 * Only one request handler is allowed for each request type. Subtypes of the
 * request type are also handled if there is no other more specific request handler for the type.
 *
 * @see HandlerLifespan
 */
@Target(AnnotationTarget.FUNCTION)
annotation class RequestHandler(val lifespan: HandlerLifespan = HandlerLifespan.SINGLE)

/**
 * Notification handlers are called on [interfaces.Mediator.publish].
 * Many notification handlers are allowed for each request type. Subtypes of the
 * request type are also handled if there are no other more specific notification handlers for the type.
 *
 * [order] determines which handler goes first. The lower the value, the sooner the handler is called.
 *
 * @see HandlerLifespan
 * @see NotificationParallel
 */
@Target(AnnotationTarget.FUNCTION)
annotation class NotificationHandler(val lifespan: HandlerLifespan = HandlerLifespan.SINGLE,
                                     val parallel: NotificationParallel = NotificationParallel.PARALLEL,
                                     val order: Int = Int.MIN_VALUE)

/**
 * Pipeline handlers may be called on [interfaces.Mediator.send] or [interfaces.Mediator.publish].
 * There may be many pipelines for each request type. A pipeline handler will also intercept the subtypes
 * of the request type.
 *
 * [order] determines which pipeline goes first. The lower the value, the sooner the pipeline is called.
 *
 * @see HandlerLifespan
 * @see PipelineTarget
 */
@Target(AnnotationTarget.FUNCTION)
annotation class PipelineBehavior(val lifespan: HandlerLifespan = HandlerLifespan.SINGLE,
                                  val order: Int = Int.MIN_VALUE,
                                  val target: PipelineTarget = PipelineTarget.BOTH)
package annotations

@Target(AnnotationTarget.FUNCTION)
annotation class RequestHandler(val lifespan: HandlerLifespan = HandlerLifespan.SINGLE)

@Target(AnnotationTarget.FUNCTION)
annotation class NotificationHandler(val lifespan: HandlerLifespan = HandlerLifespan.SINGLE,
                                     val parallel: NotificationParallel = NotificationParallel.PARALLEL,
                                     val order: Int = Int.MIN_VALUE)

@Target(AnnotationTarget.FUNCTION)
annotation class PipelineBehavior(val lifespan: HandlerLifespan = HandlerLifespan.SINGLE,
                                  val order: Int = Int.MIN_VALUE,
                                  val target: PipelineTarget = PipelineTarget.REQUESTS)

// TODO: Should the parallelism be set to SEQUENTIAL or PARALLEL by default?
/*
* Let's say that there are multiple handlers with the same order value.
* Then the reasonable intention to assume is that their particular order does not matter in the ordering of "tier".
* So by that, we should expect two handlers to run concurrently
* I will determine the answer in the future.
 */
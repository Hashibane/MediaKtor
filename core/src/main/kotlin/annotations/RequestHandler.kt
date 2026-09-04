package annotations

@Target(AnnotationTarget.FUNCTION)
annotation class RequestHandler(val lifespan: HandlerLifespan = HandlerLifespan.SINGLE)

@Target(AnnotationTarget.FUNCTION)
annotation class NotificationHandler(val lifespan: HandlerLifespan = HandlerLifespan.SINGLE)
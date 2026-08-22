package annotations

import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION)
annotation class RequestHandler(val lifespan: KClass<out HandlerType> = HandlerType.SingleOf::class)

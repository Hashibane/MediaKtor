package annotations

import kotlin.reflect.KClass

//TODO: handle lifespans
@Target(AnnotationTarget.FUNCTION)
annotation class RequestHandler(val lifespan: KClass<out HandlerType> = HandlerType.SingleOf::class)
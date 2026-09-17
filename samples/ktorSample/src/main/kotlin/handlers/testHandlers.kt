package handlers

import annotations.HandlerLifespan
import annotations.NotificationHandler
import annotations.PipelineBehavior
import annotations.PipelineTarget
import annotations.RequestHandler
import io.ktor.util.logging.Logger

data class Request(val content: String)


@RequestHandler(HandlerLifespan.FACTORY)
fun requestOne(arg: Request, logger: Logger): String {
    logger.info("Received message on requestOne with content: ${arg.content}")
    return arg.content
}

@NotificationHandler
fun notifierOne(arg: Request, logger: Logger) {
    logger.info("Received notification on notifierOne with content: ${arg.content}")
}


@NotificationHandler
fun notifierTwo(arg: Request, logger: Logger) {
    logger.info("Received notification on notifierTwo with content: ${arg.content}")
}

 
@PipelineBehavior(order = 5, target = PipelineTarget.NOTIFICATIONS)
suspend fun verify(arg: Request, next: suspend () -> Any?) {
    if (arg.content.contains("OK")) {
        next()
    } else {
        throw RuntimeException("Message must contain OK!")
    }
}

@PipelineBehavior(target = PipelineTarget.BOTH)
suspend fun logAll(arg: Request, logger: Logger, next: suspend () -> Any?): Any? {
    logger.info("Logger received request with content: ${arg.content}")
    return next()
}
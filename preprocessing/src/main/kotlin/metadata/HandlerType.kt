package metadata

sealed interface HandlerType {
    val handlerMetadata: HandlerMetadata
}

data class RequestHandler(override val handlerMetadata: HandlerMetadata) : HandlerType

data class NotificationHandler(
    override val handlerMetadata: HandlerMetadata,
    val notificationMetadata: NotificationHandlerMetadata) : HandlerType

data class PipelineHandler(
    override val handlerMetadata: HandlerMetadata,
    val pipelineMetadata: PipelineMetadata) : HandlerType

enum class HandlerDescriptor {
    REQUEST_HANDLER,
    NOTIFICATION_HANDLER,
    PIPELINE_HANDLER
}
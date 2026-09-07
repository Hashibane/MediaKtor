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

object HandlerSorter : Comparator<HandlerType> {
    override fun compare(o1: HandlerType?, o2: HandlerType?): Int {
        if (o1 is RequestHandler)
            return -1

        if (o2 is RequestHandler)
            return 1

        if (o1 is NotificationHandler && o2 is NotificationHandler)
            return o2.notificationMetadata.order - o1.notificationMetadata.order

        if (o1 is NotificationHandler && o2 is PipelineHandler)
            return -1

        if (o1 is PipelineHandler && o2 is NotificationHandler)
            return 1

        if (o2 is PipelineHandler && o1 is PipelineHandler) {
            return o2.pipelineMetadata.order - o1.pipelineMetadata.order
        }

        return 0
    }
}
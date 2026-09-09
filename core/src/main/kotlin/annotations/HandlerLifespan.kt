package annotations

enum class HandlerLifespan {
    SINGLE,
    FACTORY
}

enum class NotificationParallel {
    SEQUENTIAL,
    PARALLEL
}

enum class PipelineTarget {
    REQUESTS,
    NOTIFICATIONS,
    BOTH
}
package metadata

import annotations.NotificationParallel

data class NotificationHandlerMetadata(val parallel: NotificationParallel, val order: Int)
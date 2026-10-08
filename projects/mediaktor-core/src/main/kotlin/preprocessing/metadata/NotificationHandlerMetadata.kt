package preprocessing.metadata

import mediaktor.core.annotations.NotificationParallel

data class NotificationHandlerMetadata(val parallel: NotificationParallel, val order: Int) : AdditionalData
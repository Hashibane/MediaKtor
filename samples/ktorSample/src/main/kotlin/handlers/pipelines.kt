package handlers

import annotations.PipelineBehavior
import annotations.PipelineTarget
import database.ItemModel
import io.ktor.util.logging.*

// Will apply to any request handlers that return ItemData? and have a subtype
// of AuthorizedRequest (or AuthorizedRequest) as their request argument.
@PipelineBehavior(target = PipelineTarget.REQUEST_MATCH)
suspend fun sterilizeItemData(arg: AuthorizedRequest, next: suspend () -> ItemModel?): ItemModel? =
    if (arg.permissions == Permission.AUTHORIZED)
        next()
    else
        next()?.copy(confidentialData = "<NOT AUTHORIZED>")


sealed interface Response
data class OK(val content: Any?) : Response
data class FAIL(val content: Any?) : Response

@PipelineBehavior(target = PipelineTarget.BOTH)
suspend fun logAllErrors(arg: Any, logger: Logger, next: suspend () -> Any?): Any? {
    try {
        return OK(next())
    } catch (e: Exception) {
        logger.error("An exception was thrown: $e")
        return FAIL("Exception thrown. The content was logged.")
    }
}
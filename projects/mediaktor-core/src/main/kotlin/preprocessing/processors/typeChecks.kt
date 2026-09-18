package preprocessing.processors

import annotations.PipelineTarget
import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.UNIT
import preprocessing.exceptions.PreprocessingException

fun verifyPipeline(functionName: String, nextReturn: TypeName, returnType: TypeName, target: PipelineTarget) {

    val any = ANY.copy(nullable = true)
    val isPassNotifications = target == PipelineTarget.NOTIFICATIONS

    if (isPassNotifications && (returnType != UNIT || nextReturn != UNIT)) {
        throw PreprocessingException(
            "Pipeline $functionName has target $target is applied to notifications." +
                    "It must return Unit. The \"next\" parameter should be of type suspend () -> Unit." +
                    "Current return type: $returnType, \"next\" return: $nextReturn"
        )
    }

    if (!target.isStrict && !isPassNotifications && (nextReturn != any || returnType != any)) {
        throw PreprocessingException("Pipeline $functionName.\n" +
                " must return Any? and the \"next\" must return Any?. " +
                "For strict return type matching use STRICT target.")
    }
}
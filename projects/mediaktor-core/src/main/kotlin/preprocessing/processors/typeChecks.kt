package preprocessing.processors

import annotations.PipelineTarget
import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.UNIT
import preprocessing.exceptions.PreprocessingException

fun verifyPipeline(functionName: String, nextReturn: TypeName, returnType: TypeName, target: PipelineTarget) {

    val any = ANY.copy(nullable = true)
    val isPassNotifications = target == PipelineTarget.NOTIFICATIONS

    if (isPassNotifications && (returnType != UNIT || nextReturn != any)) {
        throw PreprocessingException(
            "Pipeline $functionName has target $target is applied to notifications." +
                    "It must return Unit. The \"next\" parameter should be of type suspend () -> Any?." +
                    "Current return type: $returnType, \"next\" return: $nextReturn"
        )
    }

    if (!target.isStrict && (nextReturn != any || returnType != any && !isPassNotifications)) {
        throw PreprocessingException("Pipeline $functionName has target PASS.\n" +
                "It must return Any? and the \"next\" must return Any?. " +
                "For strict return type matching use STRICT target.")
    }
}
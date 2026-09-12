package processors

import annotations.PipelineTarget
import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.UNIT
import errors.PreprocessingException

fun verifyPipeline(functionName: String, requestType: TypeName, nextRequest: TypeName,
                   nextReturn: TypeName, returnType: TypeName, target: PipelineTarget) {
    if (requestType != nextRequest) {
        throw PreprocessingException("The request type must be the same as type of \"next\" parameter." +
                " Expected type suspend ($requestType) ->" +
                " $returnType on handler $functionName")
    }

    val any = ANY.copy(nullable = true)
    if (!target.isStrict && (returnType != any || nextReturn != any)) {
        throw PreprocessingException("Pipeline $functionName has target PASS.\n" +
                "It must return Any? and the \"next\" must return Any?. " +
                "For strict return type matching use STRICT targets.")
    }

    if (returnType != UNIT &&
        (target == PipelineTarget.STRICT_BOTH || target == PipelineTarget.STRICT_NOTIFICATIONS)) {
        throw PreprocessingException("Pipeline $functionName has target $target and can be applied to notifications." +
                "It must return Unit. To pass the return use PASS targets.")
    }
}
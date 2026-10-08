package preprocessing.metadata

import mediaktor.core.annotations.PipelineTarget
import com.squareup.kotlinpoet.TypeName

data class PipelineMetadata(val outputType: TypeName, val order: Int, val target: PipelineTarget) : AdditionalData
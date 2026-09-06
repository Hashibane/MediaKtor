package metadata

import com.squareup.kotlinpoet.TypeName

data class PipelineMetadata(val outputType: TypeName, val order: Int)
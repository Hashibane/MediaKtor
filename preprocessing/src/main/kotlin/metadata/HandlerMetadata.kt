package metadata

import annotations.HandlerLifespan
import com.google.devtools.ksp.symbol.KSFile
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeName

data class HandlerMetadata(
    val memberName: MemberName,
    val generatedClass: ClassName,
    val inputType: TypeName,
    val args: List<ParameterSpec>,
    val returnType: TypeName,
    val origin: KSFile?,
    val lifecycle: HandlerLifespan
)
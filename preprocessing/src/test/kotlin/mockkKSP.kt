import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSReferenceElement
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSValueArgument
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.Nullability
import io.mockk.every
import io.mockk.mockkClass

internal fun mockName(string: String): KSName {
    val name = mockkClass(KSName::class)
    every { name.asString() } returns string
    return name
}

internal fun mockDeclaration(body: KSDeclaration.(KSDeclaration) -> Unit) {
    val declaration = mockkClass(KSDeclaration::class)
    declaration.body(declaration)
}

internal fun mockClassDeclaration(body: KSClassDeclaration.(KSClassDeclaration) -> Unit) {
    val declaration = mockkClass(KSClassDeclaration::class)
    every { declaration.parentDeclaration } returns null
    declaration.body(declaration)
}

internal fun mockTypeArgument(body: KSTypeArgument.(KSTypeArgument) -> Unit) {
    val typeArg = mockkClass(KSTypeArgument::class)
    typeArg.body(typeArg)
}

internal fun mockType(body: KSType.(KSType) -> Unit) {
    val type = mockkClass(KSType::class)
    every { type.arguments } returns listOf()
    every { type.isError } returns false
    type.body(type)
}

internal fun mockTypeReference(body: KSTypeReference.(KSTypeReference) -> Unit) {
    val typeRef = mockkClass(KSTypeReference::class)
    every { typeRef.element } returns null
    typeRef.body(typeRef)
}

fun functionDeclaration(body: KSFunctionDeclaration.() -> Unit): KSFunctionDeclaration {
    val functionDeclaration = mockkClass(KSFunctionDeclaration::class)
    every { functionDeclaration.parameters } returns listOf()
    every { functionDeclaration.annotations } returns sequenceOf()
    functionDeclaration.body()
    return functionDeclaration
}

fun classDeclaration(body: KSClassDeclaration.() -> Unit): KSClassDeclaration {
    val classDeclaration = mockkClass(KSClassDeclaration::class)
    classDeclaration.body()
    return classDeclaration
}
fun annotationDefinition(body: KSAnnotation.() -> Unit): KSAnnotation {
    val annotation = mockkClass(KSAnnotation::class)
    every { annotation.arguments } returns listOf()
    annotation.body()
    return annotation
}



fun KSClassDeclaration.qualifiedName(body: () -> String) = every { qualifiedName } returns mockName(body())

fun KSClassDeclaration.packageName(body: () -> String) = every { packageName } returns mockName(body())

fun KSClassDeclaration.classKind(body: () -> ClassKind) = every { classKind } returns body()

fun KSClassDeclaration.parentClassDeclaration(body: KSClassDeclaration.() -> Unit) = mockClassDeclaration {
    body()
    every { this@parentClassDeclaration.parentDeclaration } returns this
}

fun KSType.declaration(body: KSDeclaration.() -> Unit) = mockDeclaration {
    body()
    every { this@declaration.declaration } returns this
}

fun KSType.classDeclaration(body: KSClassDeclaration.() -> Unit) =
    mockClassDeclaration {
        body()
        every { this@classDeclaration.declaration } returns this
    }

fun KSType.nullability(body: () -> Nullability) {
    val nullabilityValue = body()
    every { nullability } returns nullabilityValue
    every { isMarkedNullable } returns (nullabilityValue != Nullability.NOT_NULL)
}

fun KSType.argument(body: KSTypeArgument.() -> Unit) = mockTypeArgument {
    val typeArg = mockkClass(KSTypeArgument::class)
    typeArg.body()

    val newArgs = arguments.toMutableList()
    newArgs.add(typeArg)

    every { arguments } returns newArgs
}

fun KSTypeReference.type(body: KSType.() -> Unit) = mockType {
    body()
    every { resolve() } returns this
}

fun KSTypeReference.element(body: KSReferenceElement.() -> Unit) {
    val elementRef = mockkClass(KSReferenceElement::class)
    elementRef.body()
    every { element } returns elementRef
}

fun KSValueParameter.typeRef(body: KSTypeReference.() -> Unit) = mockTypeReference {
    body()
    every { this@typeRef.type } returns this
}

fun KSFile.packageName(body: () -> String) = every { packageName } returns mockName(body())

fun KSValueArgument.value(body: () -> Any?) = every { value } returns body()

fun KSAnnotation.annotationType(body: KSTypeReference.() -> Unit) = mockTypeReference {
    body()
    every { this@annotationType.annotationType } returns this
}

fun KSAnnotation.argument(body: KSValueArgument.() -> Unit) {
    val valueArg = mockkClass(KSValueArgument::class)
    valueArg.body()

    val newArgs = arguments.toMutableList()
    newArgs.add(valueArg)

    every { arguments } returns newArgs
}

fun KSAnnotation.shortName(body: () -> String) = every { shortName } returns mockName(body())

fun KSFunctionDeclaration.parameter(body: KSValueParameter.() -> Unit) {
    val valueParam = mockkClass(KSValueParameter::class)
    valueParam.body()

    val newParams = parameters.toMutableList()
    newParams.add(valueParam)

    every { parameters } returns newParams
}

fun KSFunctionDeclaration.returnType(body: KSTypeReference.() -> Unit) = mockTypeReference {
    body()
    every { this@returnType.returnType } returns this
}

fun KSFunctionDeclaration.simpleName(body: () -> String) = every { simpleName } returns mockName(body())

fun KSFunctionDeclaration.packageName(body: () -> String) = every { packageName } returns mockName(body())

fun KSFunctionDeclaration.containingFile(body: KSFile.() -> Unit) {
    val file = mockkClass(KSFile::class)
    file.body()
    every { this@containingFile.containingFile } returns file
}

fun KSFunctionDeclaration.annotation(body: KSAnnotation.() -> Unit) {
    val annotation = annotationDefinition(body)

    val newAnnotations = annotations.toMutableList()
    newAnnotations.add(annotation)

    every { annotations } returns newAnnotations.asSequence()
}

fun KSDeclaration.packageName(body: () -> String) = every { packageName } returns mockName(body())

fun KSDeclaration.qualifiedName(body: () -> String) = every { qualifiedName } returns mockName(body())
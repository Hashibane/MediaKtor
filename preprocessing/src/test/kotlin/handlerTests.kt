
import annotations.HandlerLifespan
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import com.google.devtools.ksp.visitor.KSValidateVisitor
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.mockkStatic
import processors.HandlerProcessor
import java.io.ByteArrayOutputStream

fun <T, U> List<T>.zipForEach(other: List<U>, body: (T, U) -> Unit) {
    return this.zip(other).forEach { body(it.first, it.second) }
}

class HandlerTest : FunSpec({
    test("request handler primitive type test") {
        val paramDeclarations = listOf(
            mockkClass(KSClassDeclaration::class)
        )

        val paramPackageName = mockkClass(KSName::class)
        every { paramPackageName.asString() } returns "paramPackage"

        val fqClassName = mockkClass(KSName::class)
        every { fqClassName.asString() } returns "paramPackage.TestClass"

        paramDeclarations.forEach {
            every { it.parentDeclaration } returns null
            every { it.classKind } returns ClassKind.CLASS
            every { it.packageName } returns paramPackageName
            every { it.qualifiedName } returns fqClassName
        }

        val paramTypes = listOf(
            mockkClass(KSType::class)
        )

        paramTypes.zipForEach(paramDeclarations) { type, decl ->
            every { type.declaration } returns decl
            every { type.nullability } returns Nullability.NOT_NULL
            every { type.arguments } returns listOf()
            every { type.isError } returns false
            every { type.isMarkedNullable } returns false
        }

        val paramTypeRefs = listOf(
            mockkClass(KSTypeReference::class)
        )

        paramTypeRefs.zipForEach(paramTypes) { ref, type ->
            every { ref.resolve() } returns type
            every { ref.element } returns mockkClass(KSClassifierReference::class)
        }

        val parameters = listOf(
            mockkClass(KSValueParameter::class)
        )

        parameters.zipForEach(paramTypeRefs) { param, type ->
            every { param.type } returns type
        }

        val returnTypeName = mockkClass(KSName::class)
        every { returnTypeName.getShortName() } returns "TestReturnClass"

        val returnPackageName = mockkClass(KSName::class)
        every { returnPackageName.asString() } returns "returnPackage"

        val returnClassName = mockkClass(KSName::class)
        every { returnClassName.asString() } returns "returnPackage.TestReturnClass"

        val returnTypeDeclaration = mockkClass(KSClassDeclaration::class)
        every { returnTypeDeclaration.packageName } returns returnPackageName
        every { returnTypeDeclaration.qualifiedName } returns returnClassName
        every { returnTypeDeclaration.parentDeclaration } returns null
        every { returnTypeDeclaration.classKind } returns ClassKind.CLASS

        val returnType = mockkClass(KSType::class)
        every { returnType.nullability } returns Nullability.NOT_NULL
        every { returnType.declaration } returns returnTypeDeclaration
        every { returnType.arguments } returns listOf()
        every { returnType.isError } returns false
        every { returnType.isMarkedNullable } returns false

        val returnTypeRef = mockkClass(KSTypeReference::class)
        every { returnTypeRef.resolve() } returns returnType
        every { returnTypeRef.element } returns null

        val funName = mockkClass(KSName::class)
        every { funName.asString() } returns "primitiveHandler"

        val packageName = mockkClass(KSName::class)
        every { packageName.asString() } returns "handlers"

        val filePackageName = mockkClass(KSName::class)
        every { filePackageName.asString() } returns "handlers"

        val sourceFile = mockkClass(KSFile::class)
        every { sourceFile.packageName } returns filePackageName

        val annotationDeclarationPackage = mockkClass(KSName::class)
        every { annotationDeclarationPackage.asString() } returns "annotations"

        val annotationFqName = mockkClass(KSName::class)
        every { annotationFqName.asString() } returns "annotations.RequestHandler"

        val annotationDeclaration = mockkClass(KSDeclaration::class)
        every { annotationDeclaration.packageName } returns annotationDeclarationPackage
        every { annotationDeclaration.qualifiedName } returns annotationFqName

        val annotationType = mockkClass(KSType::class)
        every { annotationType.declaration } returns annotationDeclaration

        val annotationTypeRef = mockkClass(KSTypeReference::class)
        every { annotationTypeRef.resolve() } returns annotationType
        every { annotationTypeRef.element } returns null

        val lifespanPackage = mockkClass(KSName::class)
        every { lifespanPackage.asString() } returns "annotations"

        val lifespanFqName = mockkClass(KSName::class)
        every { lifespanPackage.asString() } returns "annotations.HandlerLifespan"

        val lifespanClassDeclaration = mockkClass(KSClassDeclaration::class)
        every { lifespanClassDeclaration.packageName } returns lifespanPackage
        every { lifespanClassDeclaration.qualifiedName } returns lifespanFqName
        every { lifespanClassDeclaration.parentDeclaration } returns null
        every { lifespanClassDeclaration.classKind } returns ClassKind.ENUM_CLASS

        val singleFqName = mockkClass(KSName::class)
        every { lifespanPackage.asString() } returns "annotations.HandlerLifespan.SINGLE"

        val lifespanEntryDeclaration = mockkClass(KSClassDeclaration::class)
        every { lifespanEntryDeclaration.packageName } returns lifespanPackage
        every { lifespanEntryDeclaration.qualifiedName } returns singleFqName
        every { lifespanEntryDeclaration.parentDeclaration } returns lifespanClassDeclaration
        every { lifespanEntryDeclaration.classKind } returns ClassKind.ENUM_ENTRY
        every { lifespanEntryDeclaration.toString() } returns "HandlerLifespan.SINGLE"

        val valueArgName = mockkClass(KSName::class)
        every { valueArgName.asString() } returns "lifespan"

        val valueArg = mockkClass(KSValueArgument::class)
        every { valueArg.value } returns lifespanEntryDeclaration

        val annotationName = mockkClass(KSName::class)
        every { annotationName.asString() } returns "RequestHandler"

        val annotation = mockkClass(KSAnnotation::class)
        every { annotation.annotationType } returns annotationTypeRef
        every { annotation.arguments } returns listOf(valueArg)
        every { annotation.shortName } returns annotationName

        val funDeclaration = mockk<KSFunctionDeclaration>()
        every { funDeclaration.parameters } returns parameters
        every { funDeclaration.returnType } returns returnTypeRef
        every { funDeclaration.simpleName } returns funName
        every { funDeclaration.packageName } returns packageName
        every { funDeclaration.containingFile } returns sourceFile
        every { funDeclaration.annotations } returns sequenceOf(annotation)

        val outputStreams = mutableListOf<ByteArrayOutputStream>()
        repeat(3) {
            outputStreams.add(ByteArrayOutputStream())
        }

        val codeGenerator = mockkClass(CodeGenerator::class)
        every { codeGenerator.createNewFile(any(), any(), any()) } returnsMany outputStreams

        val logger = mockkClass(KSPLogger::class)

        val processor = HandlerProcessor(codeGenerator, logger)

        val resolver = mockkClass(Resolver::class)
        every { resolver.getSymbolsWithAnnotation("annotations.RequestHandler") } returns sequenceOf()

        processor.HandlerVisitor().visitFunctionDeclaration(funDeclaration, Unit)
        processor.process(resolver)

        println(outputStreams.drop(2).first().toString())
    }
})


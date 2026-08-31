
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.spyk
import processors.HandlerProcessor
import sun.security.krb5.internal.KDCOptions.with
import java.io.ByteArrayOutputStream
import java.lang.Boolean

fun <T, U> List<T>.zipForEach(other: List<U>, body: (T, U) -> Unit) {
    return this.zip(other).forEach { body(it.first, it.second) }
}

class HandlerTest : FunSpec({
    test("request handler primitive type test") {
        val paramTypeNames = listOf(
            mockkClass(KSName::class)
        )

        paramTypeNames.forEach { every { it.getShortName() } returns "String" }

        val paramDeclarations = listOf(
            mockkClass(KSClassDeclaration::class)
        )

        paramDeclarations.zipForEach(paramTypeNames) {
            decl, typeName -> every { decl.simpleName } returns typeName
        }

        val paramTypes = listOf(
            mockkClass(KSType::class)
        )

        paramTypes.zipForEach(paramDeclarations) { type, decl ->
            every { type.declaration } returns decl
            every { type.nullability } returns Nullability.NOT_NULL
        }

        val paramTypeRefs = listOf(
            mockkClass(KSTypeReference::class)
        )

        paramTypeRefs.zipForEach(paramTypes) { ref, type ->
            every { ref.resolve() } returns type
        }

        val paramNames = listOf(
            mockkClass(KSName::class)
        )

        paramNames.forEachIndexed { index, name -> every { name.getShortName() } returns "arg_$index" }

        val parameters = listOf(
            mockkClass(KSValueParameter::class)
        )

        parameters.zipForEach(paramNames.zip(paramTypeRefs)) { param, (name, type) ->
            every { param.name } returns name
            every { param.type } returns type
        }

        val returnTypeName = mockkClass(KSName::class)
        every { returnTypeName.getShortName() } returns "Unit"

        val returnTypeDeclaration = mockkClass(KSClassDeclaration::class)
        every { returnTypeDeclaration.simpleName } returns returnTypeName

        val returnType = mockkClass(KSType::class)
        every { returnType.nullability } returns Nullability.NOT_NULL
        every { returnType.declaration } returns returnTypeDeclaration

        val returnTypeRef = mockkClass(KSTypeReference::class)
        every { returnTypeRef.resolve() } returns returnType

        val funName = mockkClass(KSName::class)
        every { funName.asString() } returns "primitiveHandler"

        val packageName = mockkClass(KSName::class)
        every { packageName.asString() } returns "handlers"

        val funDeclaration = mockk<KSFunctionDeclaration>()
        every { funDeclaration.parameters } returns parameters
        every { funDeclaration.returnType } returns returnTypeRef
        every { funDeclaration.simpleName } returns funName
        every { funDeclaration.packageName } returns packageName

        mockkStatic("com.google.devtools.ksp.UtilsKt")
        with(mockk<KSFunctionDeclaration>()) {
            every {
                validate(any())
            } returns true
        }

        val outputStream = ByteArrayOutputStream()
        val codeGenerator = mockkClass(CodeGenerator::class)
        every { codeGenerator.createNewFile(any(), any(), any()) } returns outputStream

        val logger = mockkClass(KSPLogger::class)

        val processor = HandlerProcessor(codeGenerator, logger)
        processor.HandlerVisitor().visitFunctionDeclaration(funDeclaration, Unit)
    }
})


import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Nullability
import io.mockk.every
import io.mockk.mockkClass

internal fun mockName(string: String) {
    val name = mockkClass(KSName::class)
    every { name.asString() } returns string
}

fun KSClassDeclaration.qualifiedName(body: () -> String) = mockName(body())

fun KSClassDeclaration.packageName(body: () -> String) = mockName(body())

fun KSClassDeclaration.classKind(body: () -> ClassKind) = every { classKind } returns body()

fun KSClassDeclaration.parentDeclaration(body: () -> KSDeclaration?) = every { parentDeclaration } returns body()

fun KSType.declaration(body: () -> KSDeclaration) = every { declaration } returns body()

fun KSType.nullability(body: () -> Nullability) {
    val nullability_ = body()
    every { nullability } returns nullability
    every { isMarkedNullable } returns (nullability != Nullability.NOT_NULL)
}
package generators

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSValueParameter
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import errors.PreprocessingException
import kotlin.random.Random

fun generateHandler(
    codeGenerator: CodeGenerator,
    function: KSFunctionDeclaration,
    requestArg: KSValueParameter,
    args: List<KSValueParameter>,
    returnType: KSTypeReference,
    idNumber: Int
) {

    val requestTypeName = requestArg.type.toTypeName()

    //TODO: Probably can be done prettier
    val handlerName =
        "Handler__${function.simpleName.asString()}__$idNumber"

    val returnTypeName = returnType.toTypeName()
    val superInterface = ClassName("interfaces", "RequestHandler")
        .parameterizedBy(requestArg.type.toTypeName(), returnTypeName)

    val handlerDeclaration = TypeSpec.classBuilder(handlerName)
        .addSuperinterface(superInterface)

    val constructorBuilder = FunSpec.constructorBuilder()

    args.forEach {
        val propName = it.name?.asString() ?: "_"
        val typeName = it.type.toTypeName()

        constructorBuilder.addParameter(propName, typeName)

        handlerDeclaration.addProperty(
            PropertySpec.builder(propName, typeName).initializer(propName).addModifiers(KModifier.PRIVATE).build()
        )
    }

    handlerDeclaration.primaryConstructor(constructorBuilder.build())

    val injectedFunction = MemberName(function.packageName.asString(),
        function.simpleName.asString())

    val propertySpecs = handlerDeclaration.propertySpecs.toTypedArray()

    handlerDeclaration.addFunction(
        FunSpec.builder("handleRequest")
            .addModifiers(KModifier.SUSPEND, KModifier.OVERRIDE)
            .addParameter("request", requestArg.type.toTypeName())
            .returns(returnTypeName)
            .addStatement("return %M(%S, ${propertySpecs.joinToString(", ") { "%N" }})",
                injectedFunction, "request", *propertySpecs)
            .build()
    )

    val handlerClass = handlerDeclaration.build()

    val fileSpec = FileSpec.builder(function.packageName.asString(), handlerName)
        .addType(handlerClass).build()

    val sourceFile = function.containingFile
    val dependencies = if (sourceFile != null) {
        Dependencies(aggregating = false, sourceFile)
    } else {
        Dependencies.ALL_FILES
    }

    fileSpec.writeTo(codeGenerator, dependencies)
}
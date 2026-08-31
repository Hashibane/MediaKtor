package generators

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import metadata.HandlerMetadata

fun generateHandler(
    metadata: HandlerMetadata
): FileSpec {
    val superInterface = ClassName("interfaces", "RequestHandler")
        .parameterizedBy(metadata.inputType, metadata.returnType)

    val handlerDeclaration = TypeSpec.classBuilder(metadata.generatedClass.simpleName)
        .addSuperinterface(superInterface)

    val constructorBuilder = FunSpec.constructorBuilder()

    metadata.args.forEach {
        constructorBuilder.addParameter(it.name, it.type)

        handlerDeclaration.addProperty(
            PropertySpec.builder(it.name, it.type).initializer(it.name).addModifiers(KModifier.PRIVATE).build()
        )
    }

    handlerDeclaration.primaryConstructor(constructorBuilder.build())

    val propertySpecs = handlerDeclaration.propertySpecs.toTypedArray()

    handlerDeclaration.addFunction(
        FunSpec.builder("handleRequest")
            .addModifiers(KModifier.SUSPEND, KModifier.OVERRIDE)
            .addParameter("request", metadata.inputType)
            .returns(metadata.returnType)
            .addStatement("return %M(%L, ${propertySpecs.joinToString(", ") { "%N" }})",
                metadata.memberName, "request", *propertySpecs)
            .build()
    )

    val handlerClass = handlerDeclaration.build()

    val fileSpec = FileSpec.builder(metadata.memberName.packageName, metadata.generatedClass.simpleName)
        .addType(handlerClass).build()

    return fileSpec
}
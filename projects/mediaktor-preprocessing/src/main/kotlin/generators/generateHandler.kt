package generators

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import errors.PreprocessingException
import metadata.HandlerMetadata
import metadata.HandlerType
import metadata.PipelineHandler

fun generateHandler(
    handlerType: HandlerType
): FileSpec {
    val metadata = handlerType.handlerMetadata

    val handlerDeclaration = TypeSpec.classBuilder(metadata.generatedClass.simpleName)

    val constructorBuilder = FunSpec.constructorBuilder()

    val isPipeline = handlerType is PipelineHandler

    metadata.args.forEach {
        if (it.name != "next" || !isPipeline) {
            constructorBuilder.addParameter(it.name, it.type)

            handlerDeclaration.addProperty(
                PropertySpec.builder(it.name, it.type).initializer(it.name).addModifiers(KModifier.PRIVATE).build()
            )
        }
    }

    handlerDeclaration.primaryConstructor(constructorBuilder.build())

    val propertySpecs = metadata.args.toTypedArray()

    val functionDeclaration = FunSpec.builder("handleRequest")
        .addModifiers(KModifier.SUSPEND)
        .addParameter("request", metadata.inputType)

    if (isPipeline) {
        val next = handlerType.handlerMetadata.args.find { it.name == "next" }!!

        functionDeclaration.addParameter("next", next.type)
    }

    handlerDeclaration.addFunction(
        functionDeclaration.returns(metadata.returnType)
            .addStatement(
                "return %M(%L, ${propertySpecs.joinToString(",") { "%N" }})",
                metadata.memberName, "request", *propertySpecs
            )
            .build())

    val handlerClass = handlerDeclaration.build()

    val fileSpec = FileSpec.builder(metadata.memberName.packageName, metadata.generatedClass.simpleName)
        .addType(handlerClass).build()

    return fileSpec
}
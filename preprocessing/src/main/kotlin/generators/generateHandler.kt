package generators

import annotations.HandlerClass
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSDeclaration
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
import com.squareup.kotlinpoet.ksp.originatingKSFiles
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import errors.PreprocessingException
import generators.HandlerGenerator.id
import metadata.HandlerMetadata
import processors.HandlerProcessor
import kotlin.random.Random

object HandlerGenerator {
    private var _id = 0
    val id: Int
        get() {
            _id += 1
            return _id
        }

    fun CodeGenerator.generateHandler(
        metadata: HandlerMetadata
    ) {
        //TODO: Probably can be done prettier as it does so to counteract cross-package simple name collisions
        val handlerName =
            "Handler__${metadata.memberName.simpleName}__$id"

        val superInterface = ClassName("interfaces", "RequestHandler")
            .parameterizedBy(metadata.inputType, metadata.returnType)

        val handlerDeclaration = TypeSpec.classBuilder(handlerName)
            .addSuperinterface(superInterface)
            .addAnnotation(HandlerClass::class)

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

        val fileSpec = FileSpec.builder(metadata.memberName.packageName, handlerName)
            .addType(handlerClass).build()

        val sourceFile = metadata.origin
        val dependencies = if (sourceFile != null) {
            Dependencies(aggregating = false, sourceFile)
        } else {
            Dependencies.ALL_FILES
        }

        fileSpec.writeTo(this, dependencies)
    }
}
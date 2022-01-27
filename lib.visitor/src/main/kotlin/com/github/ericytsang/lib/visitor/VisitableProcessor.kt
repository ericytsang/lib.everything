//package com.github.ericytsang.lib.visitor
//
//import com.google.auto.service.AutoService
//import com.squareup.kotlinpoet.ClassName
//import com.squareup.kotlinpoet.FileSpec
//import com.squareup.kotlinpoet.FunSpec
//import com.squareup.kotlinpoet.KModifier
//import com.squareup.kotlinpoet.ParameterSpec
//import com.squareup.kotlinpoet.TypeName
//import com.squareup.kotlinpoet.TypeSpec
//import com.squareup.kotlinpoet.TypeVariableName
//import java.io.File
//import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
//import com.squareup.kotlinpoet.asTypeName
//import javax.annotation.processing.AbstractProcessor
//import javax.annotation.processing.ProcessingEnvironment
//import javax.annotation.processing.Processor
//import javax.annotation.processing.RoundEnvironment
//import javax.lang.model.SourceVersion
//import javax.lang.model.element.Element
//import javax.lang.model.element.TypeElement
//import javax.lang.model.type.TypeMirror
//
//@AutoService(Processor::class)
//class VisitableProcessor : AbstractProcessor()
//{
//    companion object
//    {
//        private const val GENERATED_ELEMENT_PACKAGE_PREFIX = "com.github.ericytsang.generated"
//        private const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
//        private val returnGenericType = TypeVariableName.invoke("Return")
//    }
//
//    override fun getSupportedAnnotationTypes() = mutableSetOf(Visitable::class.qualifiedName)
//    override fun getSupportedSourceVersion():SourceVersion = SourceVersion.latest()
//
//    private data class UnionTypeElement(
//            val wrappedValueType:TypeMirror)
//    {
//        val className:ClassName get()
//        {
//            val wrappedValueClassName = ClassName.bestGuess("$GENERATED_ELEMENT_PACKAGE_PREFIX.${wrappedValueType.asTypeName()}")
//            return ClassName.bestGuess("${wrappedValueClassName.packageName}.UnionTypeElement_${wrappedValueClassName.simpleName}")
//        }
//    }
//    private data class UnionType(
//            val nameData:Element)
//    {
//        fun visitorClassName(processingEnvironment:ProcessingEnvironment):ClassName
//        {
//            return ClassName.bestGuess("${className(processingEnvironment).canonicalName}.Visitor")
//        }
//        fun parameterizedVisitorClassName(processingEnvironment:ProcessingEnvironment):TypeName
//        {
//            return visitorClassName(processingEnvironment).parameterizedBy(returnGenericType)
//        }
//        fun className(processingEnvironment:ProcessingEnvironment):ClassName
//        {
//            val wrappedValueClassName = ClassName.bestGuess("$GENERATED_ELEMENT_PACKAGE_PREFIX.${nameData.qualifiedName(processingEnvironment)}")
//            return ClassName.bestGuess("${wrappedValueClassName.packageName}.UnionType_${wrappedValueClassName.simpleName}")
//        }
//    }
//
//    /**
//     * use this to create the visited element in the visitor pattern.
//     */
//    private data class UnionTypeElementHierarchy(
//            val unionTypeElement:UnionTypeElement,
//            val superTypes:Set<UnionType>)
//    {
//        fun toFunSpec():FunSpec
//        {
//            return FunSpec
//                    .builder("asUnionType")
//                    .receiver(unionTypeElement.wrappedValueType.asTypeName())
//                    .returns(unionTypeElement.className)
//                    .addStatement("return ${unionTypeElement.className.simpleName}(this)")
//                    .build()
//        }
//
//        fun toTypeSpec(processingEnvironment:ProcessingEnvironment):TypeSpec
//        {
//            return TypeSpec.classBuilder(unionTypeElement.className)
//                    .apply {
//                        superTypes.map {it.nameData.simpleName}
//                        superTypes
//                                .map {it.className(processingEnvironment)}
//                                .forEach {addSuperinterface(it)}
//                    }
//                    .primaryConstructor(FunSpec
//                            .constructorBuilder()
//                            .addParameter("_value",unionTypeElement.wrappedValueType.asTypeName())
//                            .addStatement("value = _value")
//                            .build())
//                    .addProperty("value",unionTypeElement.wrappedValueType.asTypeName(),KModifier.PUBLIC)
//                    .addFunctions(superTypes.map {
//                        FunSpec
//                                .builder("accept")
//                                .addModifiers(KModifier.OVERRIDE)
//                                .addTypeVariable(returnGenericType)
//                                .addParameter("visitor",it.parameterizedVisitorClassName(processingEnvironment))
//                                .returns(returnGenericType)
//                                .addStatement("return visitor.visit(value)")
//                                .build()
//                    })
//                    .build()
//        }
//    }
//
//    /**
//     * use this to create the union type in the visitor pattern.
//     */
//    private data class UnionTypeVisitorHierarchy(
//            val unionType:UnionType,
//            val elements:Set<UnionTypeElementHierarchy>)
//    {
//        fun toTypeSpec(processingEnvironment:ProcessingEnvironment):TypeSpec
//        {
//            return TypeSpec.interfaceBuilder(unionType.className(processingEnvironment))
//                    .addFunction(FunSpec.builder("accept")
//                            .addModifiers(KModifier.ABSTRACT)
//                            .addTypeVariable(returnGenericType)
//                            .addParameter(
//                                    "visitor",
//                                    unionType.parameterizedVisitorClassName(processingEnvironment))
//                            .returns(returnGenericType)
//                            .build())
//                    .addType(TypeSpec.interfaceBuilder(unionType.visitorClassName(processingEnvironment))
//                            .addTypeVariable(returnGenericType)
//                            .addFunctions(elements
//                                    .map {it.unionTypeElement.wrappedValueType.asTypeName()}
//                                    .map {FunSpec
//                                            .builder("visit")
//                                            .addModifiers(KModifier.ABSTRACT)
//                                            .addParameter(ParameterSpec
//                                                    .builder("element",it)
//                                                    .build())
//                                            .returns(returnGenericType)
//                                            .build()})
//                            .build())
//                    .build()
//        }
//    }
//
//    override fun process(set:MutableSet<out TypeElement>,roundEnv:RoundEnvironment):Boolean
//    {
//        println("process")
//
//        val unionTypes = roundEnv.getElementsAnnotatedWith(Visitable::class.java)
//                .map {UnionType(it)}
//
//        val unionTypeHierarchies = unionTypes
//                .flatMap()
//                {
//                    unionTypeSrc ->
//                    unionTypeSrc.nameData
//                            .getAnnotation(Visitable::class.java).typeMirrors.asIterable()
//                            .map {UnionTypeElement(it)}
//                            .map {unionTypeElement -> unionTypeSrc to unionTypeElement}
//                }
//                .groupBy {it.second}
//                .mapValues {entry -> entry.value.map {it.first}}
//                .map {UnionTypeElementHierarchy(it.key,it.value.toSet())}
//
//        val unionTypeVisitors = unionTypeHierarchies
//                .flatMap {unionTypeHierarchy -> unionTypeHierarchy.superTypes.map {unionTypeHierarchy to it}}
//                .groupBy {it.second}
//                .mapValues {entry -> entry.value.map {it.first}}
//                .map {UnionTypeVisitorHierarchy(it.key,it.value.toSet())}
//
//        unionTypeVisitors.forEach()
//        {
//            unionTypeVisitor ->
//            val className = unionTypeVisitor.unionType.className(processingEnv)
//            FileSpec
//                    .builder(
//                            className.packageName,
//                            className.simpleName)
//                    .addType(unionTypeVisitor.toTypeSpec(processingEnv))
//                    .build()
//                    .writeTo(File(processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME]!!))
//        }
//
//        unionTypeHierarchies.forEach()
//        {
//            unionTypeElement ->
//            val className = unionTypeElement.unionTypeElement.className
//            FileSpec
//                    .builder(
//                            className.packageName,
//                            className.simpleName)
//                    .addFunction(unionTypeElement.toFunSpec())
//                    .addType(unionTypeElement.toTypeSpec(processingEnv))
//                    .build()
//                    .writeTo(File(processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME]!!))
//        }
//
//        return true
//    }
//}

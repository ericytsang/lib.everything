package com.github.ericytsang.lib.visitor

import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.PackageElement
import javax.lang.model.type.MirroredTypesException
import javax.lang.model.type.TypeMirror

fun Element.packageName(processingEnv:ProcessingEnvironment):PackageElement
{
    return processingEnv.elementUtils.getPackageOf(this)
}

fun Element.qualifiedName(processingEnv:ProcessingEnvironment):String
{
    return "${packageName(processingEnv)}.$simpleName"
}

val Visitable.typeMirrors:List<TypeMirror> get()
{
    try
    {
        classes
        throw Exception("you're not supposed to be here!")
    }
    catch (e:MirroredTypesException)
    {
        return e.typeMirrors
    }
}

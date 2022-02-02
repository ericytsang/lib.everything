package com.github.ericytsang.androidlib.core

import android.content.Context
import android.content.Intent
import android.os.Bundle
import java.io.Serializable
import kotlin.reflect.KClass
import kotlin.reflect.cast

interface Bundleable:Serializable

// region Bundle

private val BUNDLE_KEY:String = "${Bundleable::class.qualifiedName}.${::BUNDLE_KEY.name}"

fun Bundleable.toBundle():Bundle
{
    return Bundle().apply {
        putSerializable(BUNDLE_KEY,this@toBundle)
    }
}

inline fun <reified O:Bundleable> Bundle.toBundeable():O
{
    return toBundeable(kClass())
}

fun <O:Bundleable> Bundle.toBundeable(kClass:KClass<O>):O
{
    return kClass.cast(getSerializable(BUNDLE_KEY))
}

// endregion

// region Intent

fun Bundleable.toIntent(context:Context,component:KClass<out Context>):Intent
{
    return Intent(context,component.java).apply {
        putExtras(this@toIntent.toBundle())
    }
}

inline fun <reified O:Bundleable> Intent.toBundeable():O?
{
    return extras?.toBundeable(O::class)
}

fun <O:Bundleable> Intent.toBundeable(kClass:KClass<O>):O?
{
    return extras?.toBundeable(kClass)
}

// endregion
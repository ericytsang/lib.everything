package com.github.ericytsang.lib.optional

import java.io.Serializable

sealed class Opt<Wrapped:Any>:Serializable
{
    companion object
    {
        fun <Wrapped:Any> some(wrapped:Wrapped) = Some(wrapped)
        fun <Wrapped:Any> none() = None<Wrapped>()
        fun <Wrapped:Any> of(wrapped:Wrapped?) = wrapped?.let {some(it)}?:none<Wrapped>()
        fun <Wrapped:Any> of():Opt<Wrapped> = none()
    }
    abstract val opt:Wrapped?
    data class Some<Wrapped:Any>(override val opt:Wrapped):Opt<Wrapped>()
    class None<Wrapped:Any>:Opt<Wrapped>()
    {
        override val opt:Wrapped? get() = null
        override fun equals(other:Any?) = other is None<*>
        override fun hashCode() = 0
        override fun toString() = "${this::class.simpleName}()"
    }
}

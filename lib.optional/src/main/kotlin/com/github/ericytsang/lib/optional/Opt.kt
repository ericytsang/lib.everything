package com.github.ericytsang.lib.optional

import java.io.Serializable

sealed class Opt<Wrapped>:Serializable
{
    companion object
    {
        fun <Wrapped> some(wrapped:Wrapped) = Some(wrapped)
        fun <Wrapped> none() = None<Wrapped>()
        fun <Wrapped:Any> of(wrapped:Wrapped?) = wrapped?.let {some(it)}?:none<Wrapped>()
        fun <Wrapped:Any> of():Opt<Wrapped> = none()
    }
    abstract val opt:Wrapped?
    data class Some<Wrapped>(override val opt:Wrapped):Opt<Wrapped>()
    class None<Wrapped>:Opt<Wrapped>()
    {
        override val opt:Wrapped? get() = null
        override fun equals(other:Any?) = other is None<*>
        override fun hashCode() = 0
        override fun toString() = "${this::class.simpleName}()"
    }
}

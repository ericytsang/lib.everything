package com.github.ericytsang.lib.optional

import java.io.Serializable

sealed class SerializableOpt<Wrapped:Serializable>:Serializable
{
    companion object
    {
        fun <Wrapped:Serializable> some(wrapped:Wrapped) = Some(wrapped)
        fun <Wrapped:Serializable> none() = None<Wrapped>()
        fun <Wrapped:Serializable> of(wrapped:Wrapped?) = wrapped?.let {some(it)}?:none<Wrapped>()
        fun <Wrapped:Serializable> of(wrapped:Wrapped) = some(wrapped)
        fun <Wrapped:Serializable> of() = none<Wrapped>()
    }
    abstract val opt:Wrapped?
    data class Some<Wrapped:Serializable>(override val opt:Wrapped):SerializableOpt<Wrapped>()
    class None<Wrapped:Serializable>:SerializableOpt<Wrapped>()
    {
        override val opt:Wrapped? get() = null
        override fun equals(other:Any?) = other is None<*>
        override fun hashCode() = 0
        override fun toString() = "${this::class.simpleName}()"
    }
}

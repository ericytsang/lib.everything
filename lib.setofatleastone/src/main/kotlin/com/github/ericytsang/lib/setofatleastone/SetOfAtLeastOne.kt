package com.github.ericytsang.lib.setofatleastone

import java.io.Serializable

class SetOfAtLeastOne<out T>
private constructor(
        private val values:Set<T>)
    :Set<T> by values,Serializable
{
    companion object
    {
        fun <T> of(first:T,vararg theRest:T):SetOfAtLeastOne<T>
        {
            return SetOfAtLeastOne(setOf(first)+theRest)
        }
        fun <T> of(first:T,theRest:Iterable<T>):SetOfAtLeastOne<T>
        {
            return SetOfAtLeastOne(setOf(first)+theRest)
        }
        @Deprecated("try to use {of} instead")
        fun <T> runtimeCheckedOf(theRest:Iterable<T>):SetOfAtLeastOne<T>
        {
            return SetOfAtLeastOne(theRest.toSet())
        }
    }

    override fun equals(other: Any?): Boolean {
        return other is SetOfAtLeastOne<*> && other.values == values
    }

    override fun hashCode(): Int {
        return values.hashCode()
    }

    override fun toString(): String {
        return values.toString()
    }
}
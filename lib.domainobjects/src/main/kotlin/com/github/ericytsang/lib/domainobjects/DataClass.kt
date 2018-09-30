package com.github.ericytsang.lib.domainobjects

import kotlin.reflect.full.safeCast

abstract class DataClass(private val dataMembers:List<Any>):DataObject
{
    final override fun equals(other:Any?):Boolean
    {
        return this::class.safeCast(other)
                ?.dataMembers
                ?.let {it == dataMembers}
                ?:false
    }

    final override fun hashCode():Int
    {
        return dataMembers.hashCode()
    }

    final override fun toString():String
    {
        return this::class.simpleName+dataMembers.joinToString(",","(",")")
    }
}
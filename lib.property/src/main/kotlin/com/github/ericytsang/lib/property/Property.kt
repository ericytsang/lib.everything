package com.github.ericytsang.lib.property

import com.github.ericytsang.lib.closeablegroup.CloseableGroup

interface MutableProperty<Value>:Property<Value>
{
    fun set(value:Value)
}

interface Property<out Value>
{
    val listeners:Set<Any>
    fun listen(activeScope:CloseableGroup.AddCloseablesScope,onChanged:(Value)->Unit)
}
package com.github.ericytsang.lib.prop

interface MutableProp<ReadContext:Any,WriteContext:Any,Value:Any>:ReadOnlyProp<ReadContext,Value>
{
    fun set(readContext:ReadContext,writeContext:WriteContext,value:Value)
}

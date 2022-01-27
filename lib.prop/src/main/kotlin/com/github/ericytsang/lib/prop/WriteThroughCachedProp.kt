package com.github.ericytsang.lib.prop

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WriteThroughCachedProp<Context:Any,Value:Any>(
        val underlyingProp:MutableProp<Context,Value>)
    :Prop<Context,Value>()
{
    override fun asFlow():Flow<(Context) -> Value> = underlyingProp.asFlow().map {it}

    private var isDirty = false
    private var field:Value? = null
        set(value)
        {
            isDirty = true
            field = value
        }
    override fun doGet(context:Context):Value
    {
        if (!isDirty)
        {
            field = underlyingProp[context]
        }
        return field as Value
    }
    override fun doSet(context:Context,value:Value)
    {
        underlyingProp[context] = value
        isDirty = false
    }
}

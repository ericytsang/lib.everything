package com.github.ericytsang.lib.prop

class WriteThroughCachedProp<ReadContext:Any,WriteContext:Any,Value:Any>(
        val underlyingProp:MutableProp<ReadContext,WriteContext,Value>)
    :Prop<ReadContext,WriteContext,Value>()
{
    private var isInitialized = false
    private var field:Value? = null
        set(value)
        {
            isInitialized = true
            field = value
        }
    override fun doGet(context:ReadContext):Value
    {
        if (!isInitialized)
        {
            field = underlyingProp.get(context)
        }
        return field as Value
    }
    override fun doSet(readContext:ReadContext,writeContext:WriteContext,value:Value)
    {
        underlyingProp.set(readContext,writeContext,value)
        field = underlyingProp.get(readContext)
    }
}

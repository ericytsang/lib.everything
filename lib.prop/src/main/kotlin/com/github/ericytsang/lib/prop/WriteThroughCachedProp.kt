package com.github.ericytsang.lib.prop

class WriteThroughCachedProp<Context:Any,Value:Any>(val underlyingProp:Prop<Context,Value>):Prop<Context,Value>()
{
    private var isInitialized = false
    private var field:Value? = null
        set(value)
        {
            isInitialized = true
            field = value
        }
    override fun doGet(context:Context):Value
    {
        if (!isInitialized)
        {
            field = underlyingProp.get(context)
        }
        return field as Value
    }
    override fun doSet(context:Context,value:Value)
    {
        underlyingProp.set(context,value)
        field = underlyingProp.get(context)
    }
}

package com.github.ericytsang.lib.prop

class WriteThroughCachedProp<Context:Any,Value:Any>(
        val underlyingProp:MutableProp<Context,Value>)
    :Prop<Context,Value>()
{
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
            field = underlyingProp.get(context)
        }
        return field as Value
    }
    override fun doSet(context:Context,value:Value)
    {
        underlyingProp.set(context,value)
        isDirty = false
    }
}

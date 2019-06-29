package com.github.ericytsang.lib.prop

class Debouncer<Context:Any,Value:Any>(
        private val decorated:MutableProp<Context,Value>)
    :MutableProp<Context,Value> by decorated
{
    override fun set(context:Context,value:Value):Value
    {
        if (decorated.get(context) != value)
        {
            decorated.set(context,value)
        }
        return value
    }
}

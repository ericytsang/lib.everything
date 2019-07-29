package com.github.ericytsang.lib.prop

class Debouncer<Context:Any,Value:Any>(
        private val decorated:MutableProp<Context,Value>)
    :MutableProp<Context,Value> by decorated
{
    override operator fun set(context:Context,value:Value):Value
    {
        if (decorated[context] != value)
        {
            decorated[context] = value
        }
        return value
    }
}

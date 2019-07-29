package com.github.ericytsang.lib.prop

interface MutableProp<Context:Any,Value:Any>:ReadOnlyProp<Context,Value>
{
    operator fun set(context:Context,value:Value):Value
}

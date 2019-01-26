package com.github.ericytsang.lib.prop

class DataProp<Value:Any>(initialValue:Value):Prop<Unit,Value>()
{
    private var field = initialValue
    override fun doGet(context:Unit) = field
    override fun doSet(context:Unit,value:Value) {field = value}
}

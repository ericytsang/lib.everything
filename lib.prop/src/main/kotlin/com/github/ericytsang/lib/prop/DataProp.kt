package com.github.ericytsang.lib.prop

class DataProp<Value:Any>(initialValue:Value):Prop<Unit,Unit,Value>()
{
    private var field = initialValue
    override fun doGet(context:Unit) = field
    override fun doSet(readContext:Unit,writeContext:Unit,value:Value) {field = value}
}

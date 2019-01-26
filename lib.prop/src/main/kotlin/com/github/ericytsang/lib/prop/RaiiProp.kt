package com.github.ericytsang.lib.prop

import java.io.Closeable

class RaiiProp<Value:Closeable>(initialValue:Value):Prop<Unit,()->Value>(),Closeable
{
    private var field = initialValue
    override fun doGet(context:Unit) = {field}
    override fun doSet(context:Unit,value:()->Value)
    {
        field.close()
        field = value()
    }
    override fun close() = field.close()
}
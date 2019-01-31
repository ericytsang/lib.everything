package com.github.ericytsang.lib.prop

import java.io.Closeable

class RaiiProp<Value:Closeable>(initialValue:Value):Prop<Unit,Unit,()->Value>(),Closeable
{
    private var field = initialValue
    override fun doGet(context:Unit) = {field}
    override fun doSet(readContext:Unit,writeContext:Unit,value:()->Value)
    {
        field.close()
        field = value()
    }
    override fun close() = field.close()
}
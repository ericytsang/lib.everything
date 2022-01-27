package com.github.ericytsang.lib.prop

import kotlinx.coroutines.flow.MutableStateFlow

class DataProp<Value:Any>(initialValue:Value):Prop<Unit,Value>()
{
    private val backingFlow = MutableStateFlow(initialValue)
    override fun doGet(context:Unit) = backingFlow.value
    override fun doSet(context:Unit,value:Value) {backingFlow.value = value}
}

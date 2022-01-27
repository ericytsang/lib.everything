package com.github.ericytsang.lib.prop

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class DataProp<Value:Any>(initialValue:Value):Prop<Unit,Value>()
{
    private val backingFlow = MutableStateFlow(initialValue)
    override fun asFlow():Flow<(Unit) -> Value> = backingFlow.map { { _: Unit -> it} }
    override fun doGet(context:Unit) = backingFlow.value
    override fun doSet(context:Unit,value:Value) {backingFlow.value = value}
}

package com.github.ericytsang.lib.property

import com.github.ericytsang.lib.closeablegroup.CloseableGroup

class DataProperty<Value>(initialValue:Value):MutableProperty<Value>
{
    private var _value:Value = initialValue
    var value:Value
        get() = _value
        set(value) = set(value)

    override fun set(value:Value)
    {
        _value = value
        listeners.forEach {it(value)}
    }

    override val listeners:Set<(Value)->Unit> get() = _listeners
    private val _listeners = mutableSetOf<(Value)->Unit>()

    override fun listen(activeScope:CloseableGroup.AddCloseablesScope,onChanged:(Value)->Unit)
    {
        val loggedOnChange:(Value)->Unit = {
            debugPrintln("${this::class.simpleName}.onChanged($it)")
            onChanged(it)
        }
        loggedOnChange(_value)
        _listeners += loggedOnChange
        activeScope += Closeable { _listeners -= loggedOnChange }
    }
}
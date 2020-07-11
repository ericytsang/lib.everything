package com.github.ericytsang.lib.property

import com.github.ericytsang.lib.closeablegroup.CloseableGroup
import java.io.Closeable

class EventProperty<Value>:MutableProperty<Value>
{
    override fun set(value:Value)
    {
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
        _listeners += loggedOnChange
        activeScope += Closeable { _listeners -= loggedOnChange }
    }
}
package com.github.ericytsang.lib.property

import com.github.ericytsang.lib.closeablegroup.CloseableGroup
import com.github.ericytsang.lib.optional.Opt
import java.io.Closeable

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

class NullableDataProperty<Value> private constructor():MutableProperty<Value>
{
    private var value:Opt<Value> = Opt.none()

    override fun set(value:Value)
    {
        this.value = Opt.some(value)
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
        val value = value
        if (value is Opt.Some) {
            loggedOnChange(value.opt)
        }
        _listeners += loggedOnChange
        activeScope += Closeable { _listeners -= loggedOnChange }
    }

    companion object {
        fun <Value> uninitialized() = NullableDataProperty<Value>()
        fun <Value> initialized(value: Value) = uninitialized<Value>()
            .apply {set(value)}
    }
}

package com.github.ericytsang.lib.property

import com.github.ericytsang.lib.closeablegroup.CloseableGroup
import com.github.ericytsang.lib.closeablegroup.CloseableGroup.AddCloseablesScope
import java.io.Closeable

object PropertyDebugMode {
    var debugMode = true
}

internal fun debugDo(block: () -> Unit) {
    if (PropertyDebugMode.debugMode) {
        block()
    }
}

internal fun debugPrintln(debugString: String) {
    if (PropertyDebugMode.debugMode) {
        println(debugString)
    }
}

internal fun debugPrintln(debugString: () -> String) {
    if (PropertyDebugMode.debugMode) {
        println(debugString())
    }
}

interface MutableProperty<Value>:Property<Value>
{
    fun set(value:Value)
}

interface Property<out Value>
{
    val listeners:Set<Any>
    fun listen(activeScope:AddCloseablesScope,onChanged:(Value)->Unit)
}

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

    override fun listen(activeScope:AddCloseablesScope,onChanged:(Value)->Unit)
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

class EventProperty<Value>:MutableProperty<Value>
{
    override fun set(value:Value)
    {
        listeners.forEach {it(value)}
    }

    override val listeners:Set<(Value)->Unit> get() = _listeners
    private val _listeners = mutableSetOf<(Value)->Unit>()

    override fun listen(activeScope:AddCloseablesScope,onChanged:(Value)->Unit)
    {
        val loggedOnChange:(Value)->Unit = {
            debugPrintln("${this::class.simpleName}.onChanged($it)")
            onChanged(it)
        }
        _listeners += loggedOnChange
        activeScope += Closeable { _listeners -= loggedOnChange }
    }
}

class MediatorProperty<Value>:MutableProperty<Value>
{
    val sources:Set<Any> get() = _sources.keys
    private val _sources = mutableMapOf<Any,RegisteredSource<*>>()

    private val backingProperty: MutableProperty<Value> = EventProperty()
    override val listeners:Set<Any> get() = backingProperty.listeners

    private class RegisteredSource<Value>(
            val source:Property<Value>,
            val onChanged:(Value)->Unit
    )
    {
        private var closeable = CloseableGroup()
            set(value)
            {
                field.close()
                field = value
            }
        fun startListening(activeScope:AddCloseablesScope)
        {
            closeable = CloseableGroup()
            activeScope += closeable
            closeable.addCloseables()
            {
                scope ->
                source.listen(scope,onChanged)
            }
        }
        fun stopListening() = closeable.close()
    }

    fun <I> addSource(source:Property<I>, onChanged:(I)->Unit)
    {
        val newSource = RegisteredSource(source,onChanged)
        _sources[source] = newSource
        listeningToSourcesScope.addCloseables()
        {
            // professional professional
            scope ->
            newSource.startListening(scope)
        }
    }

    fun <I> removeSource(source:Property<I>)
    {
        _sources.remove(source)?.stopListening()
    }

    override fun set(value:Value)
    {
        backingProperty.set(value)
    }

    override fun listen(activeScope:AddCloseablesScope,onChanged:(Value)->Unit)
    {
        val loggedOnChange:(Value)->Unit = {
            debugPrintln("${this::class.simpleName}.onChanged($it)")
            onChanged(it)
        }
        activeScope += Closeable()
        {
            onListenersChanged()
        }
        backingProperty.listen(activeScope, loggedOnChange)
        onListenersChanged()
    }

    private val hasListeners get() = backingProperty.listeners.isNotEmpty()
    private var listeningToSourcesScope = CloseableGroup().apply {close()}

    private fun onListenersChanged()
    {
        if (hasListeners && listeningToSourcesScope.isClosed)
        {
            val newListeningToSourcesScope = CloseableGroup()
            listeningToSourcesScope = newListeningToSourcesScope
            newListeningToSourcesScope.addCloseables()
            {
                scope ->
                _sources.values.forEach()
                {
                    it.startListening(scope)
                }
            }
        }
        else if (!hasListeners && !listeningToSourcesScope.isClosed)
        {
            listeningToSourcesScope.close()
        }
    }
}

fun <I,O> Property<I>.map(transform:(I)->O):Property<O>
{
    return MediatorProperty<O>().apply()
    {
        addSource(this@map)
        {
            set(transform(it))
        }
    }
}

fun <I,O> Property<I>.switchMap(sourceSelector:(I)->Property<O>):Property<O>
{
    return MediatorProperty<O>().apply()
    {
        var oldSource:Property<O>? = null
        addSource(this@switchMap)
        {
            input ->
            oldSource?.let {removeSource(it)}
            val newSource = sourceSelector(input)
            oldSource = newSource
            addSource(newSource)
            {
                set(it)
            }
        }
    }
}

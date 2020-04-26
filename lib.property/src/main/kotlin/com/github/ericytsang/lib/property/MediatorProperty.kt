package com.github.ericytsang.lib.property

import com.github.ericytsang.lib.closeablegroup.CloseableGroup
import com.github.ericytsang.lib.closeablegroup.CloseableGroup.AddCloseablesScope
import java.io.Closeable

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


class MediatorProperty<Value>:MutableProperty<Value>
{
    private val sources = mutableMapOf<Any,RegisteredSource<*>>()

    private val backingProperty: MutableProperty<Value> = NullableDataProperty.uninitialized()
    override val listeners:Set<Any> get() = backingProperty.listeners

    fun <I> addSource(source:Property<I>, onChanged:(I)->Unit)
    {
        removeSource(source)
        val newSource = RegisteredSource(source,onChanged)
        sources[source] = newSource
        listeningToSourcesScope.addCloseables()
        {
            // professional professional
            scope ->
            newSource.startListening(scope)
        }
    }

    fun <I> removeSource(source:Property<I>)
    {
        sources.remove(source)?.stopListening()
    }

    override fun set(value:Value)
    {
        backingProperty.set(value)
    }

    /**
     * if the root of a [MediatorProperty] is based on a [DataProperty], then upon
     * [MediatorProperty.listen]ing to the [MediatorProperty], a call to
     * [DataProperty.listen] should be made, causing the callback passed into
     * [MediatorProperty.listen] to be invoked immediately.
     */
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
        // previously had no listeners, but have at least one now
        if (hasListeners && listeningToSourcesScope.isClosed)
        {
            val newListeningToSourcesScope = CloseableGroup()
            listeningToSourcesScope = newListeningToSourcesScope
            newListeningToSourcesScope.addCloseables()
            {
                scope ->
                sources.values.forEach()
                {
                    it.startListening(scope)
                }
            }
        }

        // currently have no listeners, but had at least one before
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

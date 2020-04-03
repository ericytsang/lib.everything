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

private interface MediatorPropertyState<Value>:MutableProperty<Value> {
    fun <I> addSource(source:Property<I>, onChanged:(I)->Unit)
    fun <I> removeSource(source:Property<I>)
}

private class Inactive<Value>(initialSources:Iterable<RegisteredSource<*>>):MediatorPropertyState<Value> {

    private val sources = mutableMapOf<Any,RegisteredSource<*>>()

    init
    {
        initialSources.forEach {addSource(it)}
    }

    private fun <I> addSource(registeredSource:RegisteredSource<I>)
    {
        addSource(registeredSource.source, registeredSource.onChanged)
    }

    override fun <I> addSource(source:Property<I>, onChanged:(I)->Unit)
    {
        removeSource(source)
        sources[source] = RegisteredSource(source,onChanged)
    }

    override fun <I> removeSource(source:Property<I>)
    {
        sources.remove(source)?.stopListening()
    }

    override fun set(value:Value)
    {
        throw UnsupportedOperationException("not implemented") // todo
    }

    override val listeners:Set<Any>
        get() = TODO("Not yet implemented")

    override fun listen(activeScope:AddCloseablesScope,onChanged:(Value)->Unit)
    {
        throw UnsupportedOperationException("not implemented") // todo
    }
}

private class Active<Value>(val initialValue:Value) {

}


class MediatorProperty<Value>:MutableProperty<Value>
{
    private val sources = mutableMapOf<Any,RegisteredSource<*>>()

    private val backingProperty: MutableProperty<Value> = EventProperty()
    override val listeners:Set<Any> get() = backingProperty.listeners

    fun <I> addSource(source:Property<I>, onChanged:(I)->Unit)
    {
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

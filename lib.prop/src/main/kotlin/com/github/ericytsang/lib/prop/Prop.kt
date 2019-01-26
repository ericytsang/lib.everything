package com.github.ericytsang.lib.prop

import java.io.Closeable
import java.lang.Exception
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.withLock
import kotlin.concurrent.write

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

class DataProp<Value:Any>(initialValue:Value):Prop<Unit,Value>()
{
    private var field = initialValue
    override fun doGet(context:Unit) = field
    override fun doSet(context:Unit,value:Value) {field = value}
}

abstract class Prop<Context:Any,Value:Any>:ReadOnlyProp<Context,Value>
{
    private val readWriteLock = ReentrantReadWriteLock()
    private val notifyingListenersLock = ReentrantLock()

    override fun get(context:Context):Value
    {
        return readWriteLock.read()
        {
            doGet(context)
        }
    }

    protected abstract fun doGet(context:Context):Value

    fun set(context:Context,value:Value)
    {
        readWriteLock.write()
        {
            check(notifyingListenersLock.isLocked.not()) {throw RecursiveSettingIsNotAllowedException()}
            notifyingListenersLock.withLock()
            {
                val oldValue = doGet(context)
                val before = ReadOnlyProp.Change.Before(this,context,oldValue,value)
                listeners.forEach {it(before)}

                doSet(context,value)

                val newValue = doGet(context)
                val after = ReadOnlyProp.Change.After(this,context,oldValue,newValue)
                listeners.forEach {it(after)}
            }
        }
    }

    protected abstract fun doSet(context:Context,value:Value)

    private val listeners = mutableSetOf<(ReadOnlyProp.Change<Context,Value>)->Unit>()

    final override fun listen(context:Context,onChanged:(ReadOnlyProp.Change<Context,Value>)->Unit):Closeable
    {
        val value = get(context)
        onChanged(ReadOnlyProp.Change.After(this,context,value,value))
        return listen(onChanged)
    }

    final override fun listen(onChanged:(ReadOnlyProp.Change<Context,Value>)->Unit):Closeable
    {
        listeners += onChanged
        return Closeable {listeners -= onChanged}
    }

    class RecursiveSettingIsNotAllowedException:Exception("recursive call to set detected; recursive setting is not allowed.")
}

interface ReadOnlyProp<Context:Any,Value:Any>
{
    /**
     * returns the value for this [ReadOnlyProp].
     */
    fun get(context:Context):Value

    /**
     * will call [onChanged] before returning.
     * will call [onChanged] when [ReadOnlyProp]'s value has changed.
     * returns a [Closeable] that will unsubscribe [onChanged] from change
     * events so that it will not receive any more change events in the future.
     */
    fun listen(context:Context,onChanged:(Change<Context,Value>)->Unit):Closeable

    /**
     * will call [onChanged] when [ReadOnlyProp]'s value has changed.
     * returns a [Closeable] that will unsubscribe [onChanged] from change
     * events so that it will not receive any more change events in the future.
     */
    fun listen(onChanged:(Change<Context,Value>)->Unit):Closeable

    sealed class Change<Context:Any,Value:Any>
    {
        abstract val changedProperty:ReadOnlyProp<Context,Value>
        abstract val context:Context
        abstract val oldValue:Value
        abstract val newValue:Value
        abstract val nowValue:Value

        data class Before<Context:Any,Value:Any>(
                override val changedProperty:ReadOnlyProp<Context,Value>,
                override val context:Context,
                override val oldValue:Value,
                override val newValue:Value)
            :Change<Context,Value>()
        {
            override val nowValue:Value get() = oldValue
        }

        data class After<Context:Any,Value:Any>(
                override val changedProperty:ReadOnlyProp<Context,Value>,
                override val context:Context,
                override val oldValue:Value,
                override val newValue:Value)
            :Change<Context,Value>()
        {
            override val nowValue:Value get() = newValue
        }
    }
}

val <Value:Any> ReadOnlyProp<Unit,Value>.value:Value
    get() = get(Unit)

var <Value:Any> Prop<Unit,Value>.value:Value
    get() = get(Unit)
    set(value) { set(Unit,value) }

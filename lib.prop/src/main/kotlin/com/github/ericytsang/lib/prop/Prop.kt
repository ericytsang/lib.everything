package com.github.ericytsang.lib.prop

import java.io.Closeable
import java.lang.Exception
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.withLock
import kotlin.concurrent.write

abstract class Prop<Context:Any,Value:Any>:MutableProp<Context,Value>
{
    private val readWriteLock = ReentrantReadWriteLock()
    private val notifyingListenersLock = ReentrantLock()
    private val valueIsBeingUnset = ReentrantLock()

    final override fun get(context:Context):Value
    {
        return readWriteLock.read()
        {
            doGet(context)
        }
    }

    final override fun getNullable(context:Context):Value?
    {
        return readWriteLock.write()
        {
            if (valueIsBeingUnset.isHeldByCurrentThread)
            {
                null
            }
            else
            {
                doGet(context)
            }
        }
    }

    protected abstract fun doGet(context:Context):Value

    override fun set(context:Context,value:Value)
    {
        readWriteLock.write()
        {
            check(notifyingListenersLock.isHeldByCurrentThread.not()) {throw RecursiveSettingIsNotAllowedException()}
            notifyingListenersLock.withLock()
            {
                val oldValue = lazy {doGet(context)}
                val before = lazy {ReadOnlyProp.Change.Before(this,context,oldValue.value,value)}
                valueIsBeingUnset.withLock()
                {
                    listeners.forEach {it(before.value)}
                }

                doSet(context,value)

                val newValue = lazy {doGet(context)}
                val after = lazy {ReadOnlyProp.Change.After(this,context,oldValue.value,newValue.value)}
                listeners.forEach {it(after.value)}
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

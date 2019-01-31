package com.github.ericytsang.lib.prop

import java.io.Closeable
import java.lang.Exception
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.withLock
import kotlin.concurrent.write

abstract class Prop<ReadContext:Any,WriteContext:Any,Value:Any>:MutableProp<ReadContext,WriteContext,Value>
{
    private val readWriteLock = ReentrantReadWriteLock()
    private val notifyingListenersLock = ReentrantLock()
    private val valueIsBeingUnset = ReentrantLock()

    final override fun get(context:ReadContext):Value
    {
        return readWriteLock.read()
        {
            doGet(context)
        }
    }

    final override fun getNullable(context:ReadContext):Value?
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

    protected abstract fun doGet(context:ReadContext):Value

    override fun set(readContext:ReadContext,writeContext:WriteContext,value:Value)
    {
        readWriteLock.write()
        {
            check(notifyingListenersLock.isHeldByCurrentThread.not()) {throw RecursiveSettingIsNotAllowedException()}
            notifyingListenersLock.withLock()
            {
                val oldValue = lazy {doGet(readContext)}
                val before = lazy {ReadOnlyProp.Change.Before(this,readContext,oldValue.value,value)}
                valueIsBeingUnset.withLock()
                {
                    listeners.forEach {it(before.value)}
                }

                doSet(readContext,writeContext,value)

                val newValue = lazy {doGet(readContext)}
                val after = lazy {ReadOnlyProp.Change.After(this,readContext,oldValue.value,newValue.value)}
                listeners.forEach {it(after.value)}
            }
        }
    }

    protected abstract fun doSet(readContext:ReadContext,writeContext:WriteContext,value:Value)

    private val listeners = mutableSetOf<(ReadOnlyProp.Change<ReadContext,Value>)->Unit>()

    final override fun listen(context:ReadContext,onChanged:(ReadOnlyProp.Change<ReadContext,Value>)->Unit):Closeable
    {
        val value = get(context)
        onChanged(ReadOnlyProp.Change.After(this,context,value,value))
        return listen(onChanged)
    }

    final override fun listen(onChanged:(ReadOnlyProp.Change<ReadContext,Value>)->Unit):Closeable
    {
        listeners += onChanged
        return Closeable {listeners -= onChanged}
    }

    class RecursiveSettingIsNotAllowedException:Exception("recursive call to set detected; recursive setting is not allowed.")
}

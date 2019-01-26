package com.github.ericytsang.lib.prop

import java.io.Closeable
import java.lang.Exception
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.withLock
import kotlin.concurrent.write

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

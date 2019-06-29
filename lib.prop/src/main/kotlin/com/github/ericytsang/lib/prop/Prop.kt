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
    private var change:ReadOnlyProp.Change<Context,Value>? = null

    final override fun get(context:Context):Value
    {
        return readWriteLock.read()
        {
            doGet(context)
        }
    }

    final override fun getChange():ReadOnlyProp.Change<Context,Value>
    {
        return readWriteLock.write()
        {
            if (notifyingListenersLock.isHeldByCurrentThread)
            {
                change!!
            }
            else
            {
                throw IllegalAccessException("only allowed to access change from notified listener")
            }
        }
    }

    protected abstract fun doGet(context:Context):Value

    override fun set(context:Context,value:Value):Value
    {
        return readWriteLock.write()
        {
            check(notifyingListenersLock.isHeldByCurrentThread.not()) {throw RecursiveSettingIsNotAllowedException()}
            notifyingListenersLock.withLock()
            {
                val oldValue = lazy {doGet(context)}
                listeners.firstOrNull()?.let {oldValue.value}

                doSet(context,value)

                val newValue = lazy {doGet(context)}
                val change = lazy {ReadOnlyProp.Change(this,context,oldValue.value,newValue.value)}
                listeners.firstOrNull()?.let {this.change = change.value}
                listeners.toList().forEach {it(change.value)}
                this.change = null
                value
            }
        }
    }

    protected abstract fun doSet(context:Context,value:Value)

    private val listeners = mutableSetOf<(ReadOnlyProp.Change<Context,Value>)->Unit>()

    final override fun listen(context:Context,onChanged:(ReadOnlyProp.Change<Context,Value>)->Unit):Closeable
    {
        val value = get(context)
        onChanged(ReadOnlyProp.Change(this,context,value,value))
        return listen(onChanged)
    }

    final override fun listen(onChanged:(ReadOnlyProp.Change<Context,Value>)->Unit):Closeable
    {
        listeners += onChanged
        return Closeable {listeners -= onChanged}
    }

    class RecursiveSettingIsNotAllowedException:Exception("recursive call to set detected; recursive setting is not allowed.")
}

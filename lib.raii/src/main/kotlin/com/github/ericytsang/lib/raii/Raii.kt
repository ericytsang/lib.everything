package com.github.ericytsang.lib.raii

import java.io.Closeable
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class Raii<Raii:Closeable>:Closeable,ReadOnlyRaii<Raii>
{
    companion object
    {
        class NoopCloseable<Wrapee:Any>(val wrapee:Wrapee):Closeable
        {
            override fun close() = Unit
        }
    }

    override var obj:Raii? = null
        private set

    private val lock = ReentrantLock()
    private val unblockWhenObjIsInitialized = lock.newCondition()

    private val listeners = mutableSetOf<ReadOnlyRaii.Listener>()

    fun open(raiiInstanceFactory:()->Raii):Raii = lock.withLock()
    {
        close()
        val raiiObject = raiiInstanceFactory()
        obj = raiiObject
        listeners.forEach {it.onAssigned(this)}
        unblockWhenObjIsInitialized.signalAll()
        return raiiObject
    }

    override fun close()
    {
        getAndClose()
    }

    fun getAndClose():Raii? = lock.withLock()
    {
        val raiiObject = obj
        obj = null
        listeners.forEach {it.onAssigned(this)}
        raiiObject?.close()
        return raiiObject
    }

    override fun blockingGetNonNullObj():Raii = lock.withLock()
    {
        val returnedObj:Raii
        while (true)
        {
            val obj = this.obj
            if (obj != null)
            {
                returnedObj = obj
                break
            }
            else
            {
                unblockWhenObjIsInitialized.await()
            }
        }
        returnedObj
    }

    override fun <Return:Any> doIfOpen(block:(Raii)->Return):Return? = lock.withLock()
    {
        block(obj?:return@withLock null)
    }

    override fun listen(listener:ReadOnlyRaii.Listener):Closeable = lock.withLock()
    {
        listeners += listener
        listener.onAssigned(this)
        Closeable()
        {
            listeners -= listener
        }
    }
}

interface ReadOnlyRaii<Raii:Closeable>
{
    val obj:Raii?
    fun blockingGetNonNullObj():Raii
    fun <Return:Any> doIfOpen(block:(Raii)->Return):Return?
    fun listen(listener:Listener):Closeable

    interface Listener
    {
        fun onAssigned(source:Raii<*>)
    }
}

fun <Raii:Closeable> ReadOnlyRaii<Raii>.listen(listener:(ReadOnlyRaii<*>)->Unit):Closeable
{
    val listenerObject = object:ReadOnlyRaii.Listener
    {
        override fun onAssigned(source:com.github.ericytsang.lib.raii.Raii<*>)
        {
            listener(source)
        }
    }
    return listen(listenerObject)
}

fun Iterable<ReadOnlyRaii<*>>.listen(listener:(ReadOnlyRaii<*>)->Unit):Closeable
{
    val closeables = map {it.listen(listener)}
    return Closeable()
    {
        closeables.forEach {it.close()}
    }
}

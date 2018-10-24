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

    fun open(raiiInstanceFactory:()->Raii):Raii = lock.withLock()
    {
        close()
        val raiiObject = raiiInstanceFactory()
        obj = raiiObject
        unblockWhenObjIsInitialized.signalAll()
        return raiiObject
    }

    override fun close()
    {
        getAndClose()
    }

    fun getAndClose():Raii? = lock.withLock()
    {
        obj?.close()
        val raiiObject = obj
        obj = null
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
}

interface ReadOnlyRaii<Raii:Closeable>
{
    val obj:Raii?
    fun blockingGetNonNullObj():Raii
    fun <Return:Any> doIfOpen(block:(Raii)->Return):Return?
}

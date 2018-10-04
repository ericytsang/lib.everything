package com.github.ericytsang.multiwindow.app.android

import java.io.Closeable
import java.util.concurrent.locks.ReentrantLock

// todo: move to library?
class Raii<Raii:Closeable>:Closeable
{
    companion object
    {
        class NoopCloseable<Wrapee:Any>(val wrapee:Wrapee):Closeable
        {
            override fun close() = Unit
        }
    }

    var obj:Raii? = null

    private val lock = ReentrantLock()

    fun open(raiiInstanceFactory:()->Raii):Raii = synchronized(lock)
    {
        close()
        val raiiObject = raiiInstanceFactory()
        this.obj = raiiObject
        return@synchronized raiiObject
    }

    override fun close()
    {
        getAndClose()
    }

    fun getAndClose():Raii? = synchronized(lock)
    {
        obj?.close()
        val raiiObject = obj
        this.obj = null
        raiiObject
    }
}

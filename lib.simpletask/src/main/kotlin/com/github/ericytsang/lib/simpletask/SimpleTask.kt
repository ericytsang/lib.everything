package com.github.ericytsang.lib.simpletask

import java.io.Closeable
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class SimpleTask<I,O:Any>(val block:(I)->O):Closeable
{
    private val lock = ReentrantLock()
    private var isClosed = false

    val wasInvokedOrClosed:Boolean get() = isClosed

    operator fun invoke(i:I):O? = lock.withLock()
    {
        if (isClosed)
        {
            null
        }
        else
        {
            isClosed = true
            block(i)
        }
    }

    override fun close() = lock.withLock()
    {
        isClosed = true
    }
}

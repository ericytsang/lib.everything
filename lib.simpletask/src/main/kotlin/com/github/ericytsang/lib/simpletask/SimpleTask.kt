package com.github.ericytsang.lib.simpletask

import java.io.Closeable
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

private class SimpleTask<I,O:Any>(val block:(I)->O):Closeable
{
    private val lock = ReentrantLock()
    private var isClosed = false

    operator fun invoke(i:I):O? = lock.withLock()
    {
        if (!isClosed)
        {
            block(i)
        }
        else
        {
            null
        }
    }

    override fun close() = lock.withLock()
    {
        isClosed = true
    }
}

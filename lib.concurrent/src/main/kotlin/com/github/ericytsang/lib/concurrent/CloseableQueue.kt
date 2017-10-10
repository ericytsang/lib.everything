package com.github.ericytsang.lib.concurrent

import com.sun.jmx.remote.internal.ArrayQueue
import java.io.Closeable
import java.util.Queue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class CloseableQueue<Element:Any>(val underlyingQueue:BlockingQueue<Element>):Closeable,Collection<Element>
{
    private val qAccess = ReentrantLock()
    private val qNotEmpty = qAccess.newCondition()
    private val qNotFull = qAccess.newCondition()
    private var closeCalledRecord:CloseCalledRecord? = null

    val isClosed get() = closeCalledRecord != null

    override val size:Int get() = underlyingQueue.size

    override fun contains(element:Element):Boolean = underlyingQueue.contains(element)

    override fun containsAll(elements:Collection<Element>):Boolean = underlyingQueue.containsAll(elements)

    override fun isEmpty():Boolean = underlyingQueue.isEmpty()

    override fun iterator():Iterator<Element> = underlyingQueue.iterator()

    fun put(e:Element) = qAccess.withLock()
    {
        while (underlyingQueue.isFull() && !isClosed) qNotFull.await()
        throwIfClosed()
        qNotEmpty.signal()
        underlyingQueue.add(e)
    }

    fun take():Element = qAccess.withLock()
    {
        while (underlyingQueue.isEmpty() && !isClosed) qNotEmpty.await()
        throwIfClosed()
        qNotFull.signal()
        underlyingQueue.remove()
    }

    fun offer(e:Element):Boolean = qAccess.withLock()
    {
        throwIfClosed()
        if (!underlyingQueue.isFull())
        {
            underlyingQueue.add(e)
            qNotEmpty.signal()
            true
        }
        else
        {
            false
        }
    }

    fun poll():Element? = qAccess.withLock()
    {
        throwIfClosed()
        if (!underlyingQueue.isEmpty())
        {
            qNotFull.signal()
            underlyingQueue.remove()
        }
        else
        {
            null
        }
    }

    fun peek():Element? = qAccess.withLock()
    {
        throwIfClosed()
        underlyingQueue.lastOrNull()
    }

    fun blockingPeek():Element = qAccess.withLock()
    {
        while (underlyingQueue.isEmpty() && !isClosed) qNotEmpty.await()
        throwIfClosed()
        underlyingQueue.peek()
    }

    override fun close() = qAccess.withLock()
    {
        closeCalledRecord = CloseCalledRecord()
        qNotEmpty.signalAll()
        qNotFull.signalAll()
    }

    class CloseCalledRecord internal constructor(
        val callingThread:Thread = Thread.currentThread())
        :Exception("thread that called close is: $callingThread")

    class ClosedException internal constructor(
        cause:CloseCalledRecord)
        :Exception("resource closed",cause)

    private fun <Element> Queue<Element>.isFull():Boolean
    {
        return underlyingQueue.remainingCapacity() == 0
    }

    private fun throwIfClosed()
    {
        val closeCalledRecord = closeCalledRecord
        if (closeCalledRecord != null)
        {
            throw ClosedException(closeCalledRecord)
        }
    }
}
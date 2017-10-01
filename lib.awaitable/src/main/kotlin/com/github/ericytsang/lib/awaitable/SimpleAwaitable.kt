package com.github.ericytsang.lib.awaitable

import com.github.ericytsang.lib.randomstream.RandomInputStream
import java.io.DataInputStream
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class SimpleAwaitable(initialUpdateStamp:Long? = null):Awaitable
{
    companion object
    {
        private val randomDataInputStream = DataInputStream(RandomInputStream())
        private fun randomLong():Long = randomDataInputStream.readLong()
    }

    private val lock = ReentrantLock()

    private val signalledOnUpdated = lock.newCondition()

    override var updateStamp:Long = initialUpdateStamp ?: randomLong()
        private set

    private val awaiters = LinkedHashSet<Awaiter>()

    override fun awaitUpdate(updateStamp:Long?):Long = lock.withLock()
    {
        // do not block if there is already a discrepancy between update stamps
        if (updateStamp != this.updateStamp)
        {
            this.updateStamp
        }

        // block until the update stamp changes otherwise
        else
        {
            // register so we can get the result update stamp that triggered the
            // method to become unblocked
            val awaiter = SimpleAwaitable.Awaiter(updateStamp)
            awaiters += awaiter

            // wait for the update stamp to change
            while (this.updateStamp == updateStamp) signalledOnUpdated.await()

            // retrieve the update stamp that caused the condition to unblock
            awaiter.q.take()
        }
    }

    fun signalUpdated(newUpdateStamp:Long = randomLong()) = lock.withLock()
    {
        updateStamp = newUpdateStamp
        awaiters
            .asSequence()
            .filter {it.updateStamp != newUpdateStamp}
            .forEach {it.q.put(updateStamp)}
        awaiters
            .removeAll {it.updateStamp != newUpdateStamp}
        signalledOnUpdated.signalAll()
    }

    private class Awaiter(val updateStamp:Long?)
    {
        val q = ArrayBlockingQueue<Long>(1)
    }
}

package com.github.ericytsang.lib.awaitable

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock
import kotlin.system.measureTimeMillis

abstract class SimpleAwaiter private constructor(
    val awaitable:Awaitable,
    initialUpdateStamp:Long? = null,
    val maxConsecutiveImmediateUpdates:Int = SimpleAwaiter.Companion.MAX_CONSECUTIVE_IMMEDIATE_UPDATES,
    val immediateUpdateIntervalThreshold:Long = SimpleAwaiter.Companion.IMMEDIATE_UPDATE_INTERVAL_THRESHOLD):Awaiter
{
    companion object
    {
        private const val MAX_CONSECUTIVE_IMMEDIATE_UPDATES = 100
        private const val IMMEDIATE_UPDATE_INTERVAL_THRESHOLD = 1000L

        fun fromLambda(awaitable:Awaitable,
            initialUpdateStamp:Long? = null,
            maxConsecutiveImmediateUpdates:Int = SimpleAwaiter.Companion.MAX_CONSECUTIVE_IMMEDIATE_UPDATES,
            immediateUpdateIntervalThreshold:Long = SimpleAwaiter.Companion.IMMEDIATE_UPDATE_INTERVAL_THRESHOLD,
            block:(Long)->Unit):Awaiter
        {
            val awaiter = object:SimpleAwaiter(awaitable,initialUpdateStamp,maxConsecutiveImmediateUpdates,immediateUpdateIntervalThreshold)
            {
                override fun onSignal(updateStamp:Long) = block(updateStamp)
            }
            awaiter.worker.start()
            return awaiter
        }

        fun fromLambda(vararg awaitables:Awaitable,
            maxConsecutiveImmediateUpdates:Int = SimpleAwaiter.Companion.MAX_CONSECUTIVE_IMMEDIATE_UPDATES,
            immediateUpdateIntervalThreshold:Long = SimpleAwaiter.Companion.IMMEDIATE_UPDATE_INTERVAL_THRESHOLD,
            block:(Long)->Unit):Awaiter
        {
            val signalledByAny = SimpleAwaitable()
            val signallers = awaitables.map {SimpleAwaiter.Companion.Signaller(it,signalledByAny)}
            val rootAwaiter = object:SimpleAwaiter(signalledByAny,null,maxConsecutiveImmediateUpdates,immediateUpdateIntervalThreshold)
            {
                override fun onSignal(updateStamp:Long) = block(updateStamp)
                override fun close()
                {
                    signallers.forEach {it.close()}
                    super.close()
                }
            }
            (signallers+rootAwaiter).forEach {it.worker.start()}
            return rootAwaiter
        }

        private class Signaller(awaitable:Awaitable,val toSignal:SimpleAwaitable):SimpleAwaiter(awaitable,awaitable.updateStamp,SimpleAwaiter.Companion.MAX_CONSECUTIVE_IMMEDIATE_UPDATES,0)
        {
            override fun onSignal(updateStamp:Long)
            {
                toSignal.signalUpdated()
            }
        }
    }

    private val createRecord = CreateRecord()

    class CreateRecord internal constructor():Exception()

    private val closeLock = ReentrantLock()

    internal val worker = thread(start = false)
    {
        var consecutiveLoopCount = 0
        var lastStamp:Long? = initialUpdateStamp
        while (true)
        {
            try
            {
                val timeElapsed = measureTimeMillis {
                    lastStamp = awaitable.awaitUpdate(lastStamp)
                }

                // watchdog mechanism that throws an exception if the worker is
                // looping "too much".
                if (timeElapsed < immediateUpdateIntervalThreshold)
                {
                    if (++consecutiveLoopCount > maxConsecutiveImmediateUpdates)
                    {
                        throw RuntimeException("awaiter looping too much!",createRecord)
                    }
                }
                else
                {
                    consecutiveLoopCount = 0
                }
            }
            catch (ex:InterruptedException)
            {
                break
            }
            closeLock.withLock {
                if (Thread.interrupted()) return@thread
                onSignal(lastStamp!!)
            }
        }
    }

    override fun close()
    {
        closeLock.withLock {worker.interrupt()}
        worker.join()
    }
}

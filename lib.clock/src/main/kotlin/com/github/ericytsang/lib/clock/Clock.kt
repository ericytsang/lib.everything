package com.github.ericytsang.lib.clock

import com.github.ericytsang.lib.liablethread.LiabilityEnforcingThreadFactory
import java.io.Closeable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import kotlin.math.roundToLong
import kotlin.system.measureTimeMillis

class Clock(
        _tickIntervalMillis:Long,
        threadFactory:ThreadFactory = LiabilityEnforcingThreadFactory(Executors.defaultThreadFactory()),
        private val elapsedTimeCalculator:(()->Unit)->Long = {measureTimeMillis(it)},
        private val awaitLatch:(CountDownLatch,Long)->Unit = {latch,millis->latch.await(millis,TimeUnit.MILLISECONDS)},
        private val tick:()->Unit)
    :Closeable,ReadOnlyClock
{
    companion object
    {
        fun fromFrequency(
                ticksPerSecond:Float,
                threadFactory:ThreadFactory = LiabilityEnforcingThreadFactory(Executors.defaultThreadFactory()),
                elapsedTimeCalculator:(()->Unit)->Long = {measureTimeMillis(it)},
                awaitLatch:(CountDownLatch,Long)->Unit = {latch,millis->latch.await(millis,TimeUnit.MILLISECONDS)},
                tick:()->Unit)
                :Clock
        {
            val clock = Clock(Long.MAX_VALUE,threadFactory,elapsedTimeCalculator,awaitLatch,tick)
            clock.ticksPerSecond = ticksPerSecond
            return clock
        }
    }

    private var unblockWhenTickIntervalChangesOrClosed = CountDownLatch(1)
    override var tickIntervalMillis:Long = _tickIntervalMillis
        set(value)
        {
            unblockWhenTickIntervalChangesOrClosed.countDown()
            unblockWhenTickIntervalChangesOrClosed = CountDownLatch(1)
            field = value
        }

    override var ticksPerSecond:Float
        get() = 1000f/tickIntervalMillis
        set(value) {tickIntervalMillis = (1000f/value).roundToLong()}

    private var isClosed = false
    override fun close()
    {
        isClosed = true
        unblockWhenTickIntervalChangesOrClosed.countDown()
        if (Thread.currentThread() != worker)
        {
            worker.join()
        }
    }

    private val worker = threadFactory.newThread()
    {
        var elapsedTimeMillisSinceLastCallToTick = 0L
        while (!isClosed)
        {
            val elapsedTime = elapsedTimeCalculator()
            {
                // call tick until we have caught up with elapsed time
                val tickIntervalMillis = tickIntervalMillis
                if (elapsedTimeMillisSinceLastCallToTick >= tickIntervalMillis)
                {
                    elapsedTimeMillisSinceLastCallToTick -= tickIntervalMillis
                    tick()
                }

                // wait until it is time to call tick again
                val timeToWait = tickIntervalMillis-elapsedTimeMillisSinceLastCallToTick
                if (timeToWait > 0)
                {
                    awaitLatch(unblockWhenTickIntervalChangesOrClosed,timeToWait)
                }
            }
            elapsedTimeMillisSinceLastCallToTick += elapsedTime
        }
    }

    init
    {
        worker.start()
    }
}

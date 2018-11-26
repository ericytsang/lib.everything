package com.github.ericytsang.lib.clock

import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.any
import org.mockito.Mockito.never
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Phaser
import java.util.concurrent.ThreadFactory
import kotlin.concurrent.thread

class ClockTest
{
    private val unlatchToLetThreadStart = CountDownLatch(1)
    private val unblockedWhenWorkerThreadIsDead = Phaser(1)
    private val threadFactory = ThreadFactory()
    {
        thread(start = false)
        {
            unblockedWhenWorkerThreadIsDead.register()
            unlatchToLetThreadStart.await()
            it.run()
            unblockedWhenWorkerThreadIsDead.arrive()
        }
    }

    private val elapsedTimeCalculator = spy(MockElapsedTimeCalculator())
    private val tick = spy(MockFunction0())
    private val awaitLatch = spy(MockLatchAwaiter())

    @Test
    fun changing_the_interval_from_a_long_one_to_a_short_one_makes_tick_get_called_right_away()
    {
        val clock = Clock(100,threadFactory,elapsedTimeCalculator,awaitLatch,tick)

        `when`(elapsedTimeCalculator.invoke(any()))
                .thenAnswer()
                {
                    invocation ->
                    invocation.callRealMethod()
                    50L
                }
                .thenAnswer()
                {
                    invocation ->
                    invocation.callRealMethod()
                    0L
                }
        var unblockWhenWeHaveLoopedAFewTimes = CountDownLatch(5)
        `when`(awaitLatch.invoke(any(),any())).thenAnswer {unblockWhenWeHaveLoopedAFewTimes.countDown()}

        // let the clock thread start running
        unlatchToLetThreadStart.countDown()

        // it has looped enough times. change the tick interval, and see what happens
        unblockWhenWeHaveLoopedAFewTimes.await()
        clock.tickIntervalMillis = 50

        // wait for a few more loops
        unblockWhenWeHaveLoopedAFewTimes = CountDownLatch(5)

        // enough looping. time to end it
        unblockWhenWeHaveLoopedAFewTimes.await()
        clock.close()
        unblockedWhenWorkerThreadIsDead.arriveAndAwaitAdvance()

        // verify
        verify(tick).invoke()
    }

    @Test
    fun tick_wont_get_called_if_not_enough_time_elapses()
    {
        val clock = Clock(100,threadFactory,elapsedTimeCalculator,awaitLatch,tick)

        `when`(elapsedTimeCalculator.invoke(any()))
                .thenAnswer()
                {
                    invocation ->
                    invocation.callRealMethod()
                    50L
                }
                .thenAnswer()
                {
                    invocation ->
                    invocation.callRealMethod()
                    0L
                }
        val unblockWhenWeHaveLoopedAFewTimes = CountDownLatch(5)
        `when`(awaitLatch.invoke(any(),any())).thenAnswer {unblockWhenWeHaveLoopedAFewTimes.countDown()}

        // let the clock thread start running
        unlatchToLetThreadStart.countDown()

        // it has looped enough times. time to kill it
        unblockWhenWeHaveLoopedAFewTimes.await()
        clock.close()
        unblockedWhenWorkerThreadIsDead.arriveAndAwaitAdvance()

        // verify
        verify(tick,never()).invoke()
    }

    @Test
    fun tick_gets_called_multiple_times_to_catch_up_with_elapsed_time()
    {
        val clock = Clock(100,threadFactory,elapsedTimeCalculator,awaitLatch,tick)

        `when`(elapsedTimeCalculator.invoke(any()))
                .thenAnswer()
                {
                    invocation ->
                    invocation.callRealMethod()
                    1000L
                }
                .thenAnswer()
                {
                    invocation ->
                    invocation.callRealMethod()
                    0L
                }
        val unblockWhenWeHaveLoopedAFewTimes = CountDownLatch(15)
        `when`(awaitLatch.invoke(any(),any())).thenAnswer {unblockWhenWeHaveLoopedAFewTimes.countDown()}

        // let the clock thread start running
        unlatchToLetThreadStart.countDown()

        // it has looped enough times. time to kill it
        unblockWhenWeHaveLoopedAFewTimes.await()
        clock.close()
        unblockedWhenWorkerThreadIsDead.arriveAndAwaitAdvance()

        // verify
        verify(tick,times(10)).invoke()
    }
}

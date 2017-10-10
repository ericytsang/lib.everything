package com.github.ericytsang.lib.concurrent

import com.github.ericytsang.lib.testutils.TestUtils.exceptionExpected
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ErrorCollector
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import kotlin.concurrent.thread
import kotlin.system.measureTimeMillis

class StaticFunsTests
{
    @JvmField
    @Rule
    val errorCollector = ErrorCollector()

    @Test
    fun sleep_result_is_not_interrupted_and_duration_is_greater_than_or_equal_to_specified_sleep_duration_when_thread_is_not_interrupted_test()
    {
        val sleepDuration = measureTimeMillis()
        {
            val result = sleep(10)
            assert(result.sleepDuration >= 10)
            assert(!result.wasInterrupted)
        }
        assert(sleepDuration >= 10)
    }

    @Test
    fun sleep_result_is_interrupted_and_duration_is_less_than_specified_sleep_duration_when_thread_is_interrupted_test()
    {
        val mainThread = Thread.currentThread()
        thread {
            mainThread.awaitSuspended()
            mainThread.interrupt()
        }
        val result = sleep(Long.MAX_VALUE)
        assert(result.sleepDuration < Long.MAX_VALUE)
        assert(result.wasInterrupted)
    }

    @Test
    fun future_exception_test()
    {
        val releaseWhenMainThreadVerifiedBlocked = CountDownLatch(1)
        val f = future<Unit> {
            releaseWhenMainThreadVerifiedBlocked.await()
            throw RuntimeException("asdfasdf")
        }
        val mainThread = Thread.currentThread()
        thread()
        {
            mainThread.awaitSuspended()
            releaseWhenMainThreadVerifiedBlocked.countDown()
        }
        val thrownException = exceptionExpected {f.get()}
        assert(thrownException is ExecutionException)
    }

    @Test
    fun get_blocks_until_result_is_available()
    {
        val releaseWhenMainThreadVerifiedBlocked = CountDownLatch(1)
        val f = future {
            releaseWhenMainThreadVerifiedBlocked.await()
            Unit
        }
        val mainThread = Thread.currentThread()
        thread()
        {
            mainThread.awaitSuspended()
            releaseWhenMainThreadVerifiedBlocked.countDown()
        }
        assert(f.get() == Unit)
    }
}

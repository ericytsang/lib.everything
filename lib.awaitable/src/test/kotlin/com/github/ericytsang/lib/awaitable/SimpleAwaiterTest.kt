package com.github.ericytsang.lib.awaitable

import com.github.ericytsang.lib.concurrent.awaitSuspended
import com.github.ericytsang.lib.testutils.NoZombiesAllowed
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread

class SimpleAwaiterTest
{
    @JvmField
    @Rule
    val noZombieThreads = NoZombiesAllowed()

    @Test
    fun close_kills_worker_test()
    {
        val awaitable = SimpleAwaitable()
        val awaiter = SimpleAwaiter.fromLambda(awaitable)
        {
            println("herro")
        } as SimpleAwaiter
        assert(awaiter.worker.isAlive)
        awaiter.worker.awaitSuspended()
        awaiter.close()
        awaiter.worker.join()
    }

    @Test
    fun close_kills_all_workers_test()
    {
        val awaitable1 = SimpleAwaitable()
        val awaitable2 = SimpleAwaitable()
        val awaitable3 = SimpleAwaitable()
        val awaitable4 = SimpleAwaitable()
        val awaiter = SimpleAwaiter.fromLambda(awaitable1,awaitable2,awaitable3,awaitable4)
        {
            println("herro")
        } as SimpleAwaiter
        assert(awaiter.worker.isAlive)
        awaiter.close()
    }

    @Test
    fun any_awaitable_can_signal_lead_awaiter_test()
    {
        val awaitable1 = SimpleAwaitable()
        val awaitable2 = SimpleAwaitable()
        val awaitable3 = SimpleAwaitable()
        val awaitable4 = SimpleAwaitable()
        var count = 0
        var latch = CountDownLatch(1)
        val awaiter = SimpleAwaiter.fromLambda(awaitable1,awaitable2,awaitable3,awaitable4)
        {
            count++
            latch.countDown()
        }
        latch.await()
        latch = CountDownLatch(1)
        check(count == 1)
        awaitable1.signalUpdated()
        latch.await()
        latch = CountDownLatch(1)
        check(count == 2)
        awaitable1.signalUpdated()
        latch.await()
        latch = CountDownLatch(1)
        check(count == 3)
        awaitable2.signalUpdated()
        latch.await()
        latch = CountDownLatch(1)
        check(count == 4)
        awaitable3.signalUpdated()
        latch.await()
        latch = CountDownLatch(1)
        check(count == 5)
        awaitable4.signalUpdated()
        latch.await()
        latch = CountDownLatch(1)
        check(count == 6)
        awaitable4.signalUpdated()
        latch.await()
        latch = CountDownLatch(1)
        check(count == 7)
        awaiter.close()
    }

    @Test
    fun awaiter_test()
    {
        var signalCount = 0
        var latch = CountDownLatch(1)
        val awaitable = SimpleAwaitable()
        val awaiter = SimpleAwaiter.fromLambda(awaitable)
        {
            signalCount++
            latch.countDown()
        } as SimpleAwaiter

        latch.await()
        awaiter.worker.awaitSuspended()

        latch = CountDownLatch(1)
        awaitable.signalUpdated()

        latch.await()
        awaiter.worker.awaitSuspended()

        latch = CountDownLatch(1)
        awaitable.signalUpdated()

        latch.await()
        awaiter.worker.awaitSuspended()

        latch = CountDownLatch(1)
        awaitable.signalUpdated()

        latch.await()
        awaiter.worker.awaitSuspended()

        assert(signalCount == 4) {signalCount}
        awaiter.close()
        awaiter.worker.join()
    }

    @Test
    fun loop_too_much_test()
    {
        var signalCount = 0
        val awaitable = SimpleAwaitable()
        val awaiter = SimpleAwaiter.fromLambda(awaitable)
        {
            signalCount++
        } as SimpleAwaiter
        while (awaiter.worker.isAlive)
        {
            awaitable.signalUpdated()
        }
        awaiter.worker.join()
    }

    @Test
    fun interrupt_does_not_work_until_awaiting_test()
    {
        val callbackLatch = CountDownLatch(1)
        val awaitable = SimpleAwaitable()
        val awaiter = SimpleAwaiter.fromLambda(awaitable)
        {
            callbackLatch.await()
        } as SimpleAwaiter
        awaiter.worker.awaitSuspended()
        assert(awaiter.worker.isAlive)
        awaitable.signalUpdated()
        val t = thread {awaiter.close()}
        t.awaitSuspended()
        assert(awaiter.worker.isAlive)
        callbackLatch.countDown()
        t.join()
        awaiter.worker.join()
    }
}

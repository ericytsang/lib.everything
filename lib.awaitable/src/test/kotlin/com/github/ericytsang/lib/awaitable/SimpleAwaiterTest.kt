package com.github.ericytsang.lib.awaitable

import com.github.ericytsang.lib.testutils.TestUtils
import org.junit.After
import org.junit.Test
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread

class SimpleAwaiterTest
{
    @After
    fun teardown()
    {
        TestUtils.assertAllWorkerThreadsDead()
    }

    @Test
    fun closeKillsWorkerTest()
    {
        val awaitable = SimpleAwaitable()
        val awaiter = SimpleAwaiter.fromLambda(awaitable)
        {
            println("herro")
        } as SimpleAwaiter
        assert(awaiter.worker.isAlive)
        Thread.sleep(100)
        awaiter.close()
        Thread.sleep(100)
        assert(!awaiter.worker.isAlive)
    }

    @Test
    fun closeKillsAllWorkersTest()
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
    fun anyAwaitableSignalsLeadAwaiterTest()
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
    fun awaiterTest()
    {
        var signalCount = 0
        val awaitable = SimpleAwaitable()
        val awaiter = SimpleAwaiter.fromLambda(awaitable)
        {
            signalCount++
        } as SimpleAwaiter
        Thread.sleep(100)
        awaitable.signalUpdated()
        Thread.sleep(100)
        awaitable.signalUpdated()
        Thread.sleep(100)
        awaitable.signalUpdated()
        Thread.sleep(100)
        assert(signalCount == 4) {signalCount}
        awaiter.close()
        Thread.sleep(100)
        assert(!awaiter.worker.isAlive)
    }

    @Test
    fun loopTooMuchTest()
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
        assert(!awaiter.worker.isAlive)
    }

    @Test
    fun interruptDoesntWorkUntilAwaitingTest()
    {
        val callbackLatch = CountDownLatch(1)
        val awaitable = SimpleAwaitable()
        val awaiter = SimpleAwaiter.fromLambda(awaitable)
        {
            callbackLatch.await()
        } as SimpleAwaiter
        Thread.sleep(100)
        assert(awaiter.worker.isAlive)
        Thread.sleep(100)
        awaitable.signalUpdated()
        Thread.sleep(100)
        val t = thread {awaiter.close()}
        Thread.sleep(100)
        assert(awaiter.worker.isAlive)
        Thread.sleep(100)
        callbackLatch.countDown()
        t.join()
        Thread.sleep(100)
        assert(!awaiter.worker.isAlive)
    }
}

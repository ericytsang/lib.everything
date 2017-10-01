package com.github.ericytsang.lib.awaitable

import com.github.ericytsang.lib.concurrent.future
import com.github.ericytsang.lib.testutils.TestUtils
import org.junit.After
import org.junit.Test
import java.lang.Thread.currentThread
import java.lang.Thread.sleep

class SimpleAwaitableTest
{
    val simpleAwaitable = SimpleAwaitable()

    @After
    fun teardown()
    {
        TestUtils.assertAllWorkerThreadsDead()
    }

    @Test
    fun updateStampTest()
    {
        assert(simpleAwaitable.updateStamp == simpleAwaitable.updateStamp)
        assert(simpleAwaitable.updateStamp == simpleAwaitable.updateStamp)
    }

    @Test
    fun awaitUpdateUnblocksTest()
    {
        simpleAwaitable.awaitUpdate(simpleAwaitable.updateStamp+1)
    }

    @Test
    fun awaitUpdateBlocksTest()
    {
        val originalUpdateStamp = simpleAwaitable.updateStamp
        val blocked = future {
            val us = simpleAwaitable.awaitUpdate(simpleAwaitable.updateStamp)
            assert(us == simpleAwaitable.updateStamp)
            us
        }
        sleep(100)
        assert(!blocked.isDone)
        simpleAwaitable.signalUpdated(simpleAwaitable.updateStamp)
        sleep(100)
        assert(!blocked.isDone)
        simpleAwaitable.signalUpdated(simpleAwaitable.updateStamp)
        sleep(100)
        assert(!blocked.isDone)
        val triggeringUs = simpleAwaitable.updateStamp+1
        simpleAwaitable.signalUpdated(triggeringUs)
        sleep(100)
        assert(blocked.isDone)
        assert(originalUpdateStamp != simpleAwaitable.updateStamp)
        assert(blocked.get() == triggeringUs)
    }

    @Test
    fun interruptTest()
    {
        val originalUpdateStamp = simpleAwaitable.updateStamp
        var t = Thread()
        val blocked = future {
            t = currentThread()
            simpleAwaitable.awaitUpdate(simpleAwaitable.updateStamp)
        }
        sleep(100)
        assert(!blocked.isDone)
        t.interrupt()
        sleep(100)
        assert(blocked.isDone)
        assert(originalUpdateStamp == simpleAwaitable.updateStamp)
        val ex = TestUtils.exceptionExpected{blocked.get()}
        assert(ex.cause is InterruptedException)
    }

    @Test
    fun awaitUpdateBlocksMultipleThreadsTest()
    {
        val originalUpdateStamp = simpleAwaitable.updateStamp
        val blocked = (1..5).map {
            future {
                val us = simpleAwaitable.awaitUpdate(simpleAwaitable.updateStamp)
                assert(us == simpleAwaitable.updateStamp)
            }
        }
        sleep(100)
        assert(!blocked.all {it.isDone})
        simpleAwaitable.signalUpdated()
        sleep(100)
        assert(blocked.all {it.isDone})
        assert(originalUpdateStamp != simpleAwaitable.updateStamp)
        blocked.forEach {it.get()}
    }
}

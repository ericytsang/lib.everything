package com.github.ericytsang.lib.awaitable

import com.github.ericytsang.lib.concurrent.awaitSuspended
import com.github.ericytsang.lib.testutils.NoZombiesAllowed
import com.github.ericytsang.lib.testutils.TestUtils.exceptionExpected
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ErrorCollector
import java.util.concurrent.ArrayBlockingQueue
import kotlin.concurrent.thread

class SimpleAwaitableTest
{
    @JvmField
    @Rule
    val errorCollector = ErrorCollector()

    @JvmField
    @Rule
    val noZombieThreads = NoZombiesAllowed()

    val simpleAwaitable = SimpleAwaitable()

    @Test
    fun update_stamp_test()
    {
        assert(simpleAwaitable.updateStamp == simpleAwaitable.updateStamp)
        assert(simpleAwaitable.updateStamp == simpleAwaitable.updateStamp)
    }

    @Test
    fun await_update_unblocks_test()
    {
        simpleAwaitable.awaitUpdate(simpleAwaitable.updateStamp+1)
    }

    @Test
    fun await_update_blocks_test()
    {
        val originalUpdateStamp = simpleAwaitable.updateStamp
        val q = ArrayBlockingQueue<Long>(1)
        val blocked = thread {
            val us = simpleAwaitable.awaitUpdate(simpleAwaitable.updateStamp)
            assert(us == simpleAwaitable.updateStamp)
            q.put(us)
        }
        blocked.awaitSuspended()
        assert(blocked.isAlive)
        assert(q.isEmpty())
        simpleAwaitable.signalUpdated(simpleAwaitable.updateStamp)
        blocked.awaitSuspended()
        assert(blocked.isAlive)
        assert(q.isEmpty())
        simpleAwaitable.signalUpdated(simpleAwaitable.updateStamp)
        blocked.awaitSuspended()
        assert(blocked.isAlive)
        assert(q.isEmpty())
        val triggeringUs = simpleAwaitable.updateStamp+1
        simpleAwaitable.signalUpdated(triggeringUs)
        blocked.join()
        assert(originalUpdateStamp != simpleAwaitable.updateStamp)
        assert(q.take() == triggeringUs)
    }

    @Test
    fun interrupt_test()
    {
        val originalUpdateStamp = simpleAwaitable.updateStamp
        val t = thread {
            errorCollector.checkSucceeds {
                val ex = exceptionExpected {
                    simpleAwaitable.awaitUpdate(simpleAwaitable.updateStamp)
                }
                assert(ex is InterruptedException)
            }
        }
        t.awaitSuspended()
        assert(t.isAlive)

        t.interrupt()
        t.join()
        assert(originalUpdateStamp == simpleAwaitable.updateStamp)
    }

    @Test
    fun await_update_blocks_multiple_threads_test()
    {
        val originalUpdateStamp = simpleAwaitable.updateStamp
        val blocked = (1..5).map {
            thread {
                errorCollector.checkSucceeds {
                    val us = simpleAwaitable.awaitUpdate(simpleAwaitable.updateStamp)
                    assert(us == simpleAwaitable.updateStamp)
                }
            }
        }
        blocked.forEach {it.awaitSuspended()}
        assert(blocked.all {it.isAlive})
        simpleAwaitable.signalUpdated()
        blocked.forEach {it.join()}
        assert(originalUpdateStamp != simpleAwaitable.updateStamp)
    }
}

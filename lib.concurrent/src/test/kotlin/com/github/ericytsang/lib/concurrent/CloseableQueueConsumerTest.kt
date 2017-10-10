package com.github.ericytsang.lib.concurrent

import com.github.ericytsang.lib.testutils.TestUtils
import org.hamcrest.CoreMatchers.`is`
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ErrorCollector
import java.util.concurrent.ArrayBlockingQueue
import kotlin.concurrent.thread

class CloseableQueueConsumerTest
{
    private val ENQUEUED_VALUE = 5
    private val fixture = CloseableQueue<Int>(ArrayBlockingQueue(5))
    @JvmField
    @Rule
    val errorCollector = ErrorCollector()

    @After
    fun teardown()
    {
        TestUtils.assertAllWorkerThreadsDead()
    }

    private fun blocking_call_blocks_until_not_empty(blockingConsume:()->Unit)
    {
        val t = thread {
            errorCollector.checkSucceeds {
                blockingConsume()
            }
        }
        t.awaitSuspended()
        fixture.put(ENQUEUED_VALUE)
        t.join()
    }

    @Test
    fun take_blocking_call_blocks_until_not_empty()
    {
        val q = ArrayBlockingQueue<Int>(1)
        blocking_call_blocks_until_not_empty {q.put(fixture.take())}
        assert(q.poll() == ENQUEUED_VALUE)
        assert(fixture.isEmpty())
    }

    @Test
    fun blocking_peek_blocking_call_blocks_until_not_empty()
    {
        val q = ArrayBlockingQueue<Int>(1)
        blocking_call_blocks_until_not_empty {q.put(fixture.blockingPeek())}
        assert(q.poll() == ENQUEUED_VALUE)
        assert(fixture.size == 1)
    }

    private fun nonblocking_call_returns_when_empty(nonblockingConsume:()->Int?)
    {
        assert(nonblockingConsume() == null)
    }

    @Test
    fun poll_nonblocking_call_returns_when_empty()
    {
        nonblocking_call_returns_when_empty()
        {
            fixture.poll()
        }
    }

    @Test
    fun peek_nonblocking_call_returns_when_empty()
    {
        nonblocking_call_returns_when_empty()
        {
            fixture.peek()
        }
    }

    private fun nonblocking_call_returns_when_not_empty(nonblockingConsume:()->Int?)
    {
        fixture.put(ENQUEUED_VALUE)
        assert(nonblockingConsume() == ENQUEUED_VALUE)
    }

    @Test
    fun poll_nonblocking_call_returns_when_not_empty()
    {
        nonblocking_call_returns_when_not_empty()
        {
            fixture.poll()
        }
    }

    @Test
    fun peek_nonblocking_call_returns_when_not_empty()
    {
        nonblocking_call_returns_when_not_empty()
        {
            fixture.peek()
        }
    }

    private fun blocking_call_throws_exception_when_queue_closed(blockingConsume:()->Unit)
    {
        val t = thread {
            try
            {
                blockingConsume()
            }
            catch (ex:Exception)
            {
                errorCollector.checkThat(ex,`is`(CloseableQueue.ClosedException::class.java))
            }
        }
        t.awaitSuspended()
        fixture.close()
        t.join()
    }

    @Test
    fun take_blocking_call_throws_exception_when_queue_closed()
    {
        blocking_call_throws_exception_when_queue_closed()
        {
            fixture.take()
        }
    }

    @Test
    fun blocking_peek_blocking_call_throws_exception_when_queue_closed()
    {
        blocking_call_throws_exception_when_queue_closed()
        {
            fixture.blockingPeek()
        }
    }

    private fun nonblocking_call_throws_exception_when_queue_closed(nonblockingConsume:()->Unit)
    {
        fixture.close()
        try
        {
            nonblockingConsume()
        }
        catch (ex:Exception)
        {
            errorCollector.checkThat(ex,`is`(CloseableQueue.ClosedException::class.java))
        }
    }

    @Test
    fun poll_nonblocking_call_throws_exception_when_queue_closed()
    {
        nonblocking_call_throws_exception_when_queue_closed()
        {
            fixture.poll()
        }
    }

    @Test
    fun peek_nonblocking_call_throws_exception_when_queue_closed()
    {
        nonblocking_call_throws_exception_when_queue_closed()
        {
            fixture.peek()
        }
    }
}


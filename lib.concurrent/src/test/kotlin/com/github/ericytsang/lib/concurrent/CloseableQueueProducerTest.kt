package com.github.ericytsang.lib.concurrent

import org.junit.Rule
import org.junit.Test
import org.junit.rules.ErrorCollector
import java.util.concurrent.ArrayBlockingQueue
import kotlin.concurrent.thread

class CloseableQueueProducerTest
{
    private val ENQUEUED_VALUES = listOf(1,2,3,4,5)
    private val fixture = CloseableQueue<Int>(ArrayBlockingQueue(5))
    @JvmField
    @Rule
    val errorCollector = ErrorCollector()

    private fun puts_elements_into_queue(produce:(Int)->Unit)
    {
        ENQUEUED_VALUES.forEach(produce)
        assert(fixture.toList() == listOf(1,2,3,4,5))
    }

    @Test
    fun put_puts_elements_into_queue()
    {
        puts_elements_into_queue()
        {
            fixture.put(it)
        }
    }

    @Test
    fun offer_puts_elements_into_queue()
    {
        puts_elements_into_queue()
        {
            fixture.offer(it)
        }
    }

    @Test
    fun put_blocks_until_not_full()
    {
        put_puts_elements_into_queue()
        val t = thread()
        {
            fixture.put(6)
        }
        t.awaitSuspended()
        fixture.take()
        t.join()
        assert(fixture.toList() == listOf(2,3,4,5,6))
    }

    @Test
    fun offer_returns_when_full()
    {
        put_puts_elements_into_queue()
        assert(!fixture.offer(6))
        assert(fixture.toList() == ENQUEUED_VALUES)
    }
}

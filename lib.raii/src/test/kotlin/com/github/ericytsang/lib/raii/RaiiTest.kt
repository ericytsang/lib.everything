package com.github.ericytsang.lib.raii

import com.github.ericytsang.lib.concurrent.awaitSuspended
import org.junit.Test
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import java.io.Closeable
import java.util.concurrent.ArrayBlockingQueue
import kotlin.concurrent.thread
import kotlin.test.assertEquals
import kotlin.test.assertSame

class RaiiTest
{
    private val closeable = mock(Closeable::class.java)

    @Test
    fun get_returns_instance_provided_in_open()
    {
        val raii = Raii<Closeable>()
        raii.open {closeable}
        assertSame(closeable,raii.obj)
    }

    @Test
    fun open_closes_the_previous_object_before_opening_the_new_one()
    {
        val actions = mutableListOf<Int>()
        val raii = Raii<Closeable>()
        raii.open {
            actions.add(1)
            closeable
        }
        doAnswer {actions.add(2)}.`when`(closeable).close()
        raii.open {
            actions.add(3)
            closeable
        }
        assertEquals(listOf(1,2,3),actions)
    }

    @Test
    fun getAndClose_closes_the_object_before_it()
    {
        val raii = Raii<Closeable>()
        raii.open {closeable}
        verify(closeable,never()).close()
        assertSame(closeable,raii.getAndClose())
        verify(closeable).close()
    }

    @Test
    fun blockingGetNonNullObj_returns_once_raii_is_open()
    {
        val raii = Raii<Closeable>()
        val q = ArrayBlockingQueue<Closeable>(1)
        val t = thread {
            q.put(raii.blockingGetNonNullObj())
        }
        t.awaitSuspended()
        raii.open {closeable}
        t.join()
        assertSame(closeable,q.take())
    }

    @Test
    fun blockingGetNonNullObj_does_not_return_when_closed_then_returns_once_raii_is_open()
    {
        val raii = Raii<Closeable>()
        val q = ArrayBlockingQueue<Closeable>(1)
        val t = thread {
            q.put(raii.blockingGetNonNullObj())
        }
        t.awaitSuspended()
        raii.getAndClose()
        raii.close()
        t.awaitSuspended()
        check(t.isAlive)
        raii.open {closeable}
        t.join()
        assertSame(closeable,q.take())
    }
}

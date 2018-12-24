package com.github.ericytsang.lib.raii

import com.github.ericytsang.lib.concurrent.awaitSuspended
import org.junit.Test
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import java.io.Closeable
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.LinkedBlockingQueue
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

    @Test
    fun listener_triggered_after_open()
    {
        val q = LinkedBlockingQueue<Int>()
        val raii = Raii<Closeable>()
        raii.listen {q += 2}
        raii.open {
            q += 1
            Closeable {q += 3}
        }
        assertEquals(
                listOf(2,2,1,2),
                generateSequence {q.poll()}.toList())
    }

    @Test
    fun listener_triggered_after_open_and_before_close()
    {
        val q = LinkedBlockingQueue<Int>()
        val raii = Raii<Closeable>()
        assertEquals(listOf(),generateSequence {q.poll()}.toList())

        raii.listen {
            q += if (raii.obj != null)
            {
                2
            }
            else
            {
                3
            }
        }
        assertEquals(generateSequence {q.poll()}.toList(),listOf(3))

        raii.open {
            q += 1
            Closeable {q += 4}
        }
        assertEquals(generateSequence {q.poll()}.toList(),listOf(3,1,2))

        raii.close()
        assertEquals(generateSequence {q.poll()}.toList(),listOf(3,4))
    }

    @Test
    fun listener_on_multiple_raiis()
    {
        val q = LinkedBlockingQueue<Int>()
        val raii1 = Raii<Closeable>()
        val raii2 = Raii<Closeable>()
        assertEquals(listOf(),generateSequence {q.poll()}.toList())

        listOf(raii1,raii2).listen {
            q += if (raii1.obj != null)
            {
                1
            }
            else
            {
                2
            }
            q += if (raii2.obj != null)
            {
                3
            }
            else
            {
                4
            }
        }
        assertEquals(listOf(2,4,2,4),generateSequence {q.poll()}.toList())

        raii1.open {
            q += 5
            Closeable {q += 6}
        }
        assertEquals(listOf(2,4,5,1,4),generateSequence {q.poll()}.toList())

        raii2.open {
            q += 7
            Closeable {q += 8}
        }
        assertEquals(listOf(1,4,7,1,3),generateSequence {q.poll()}.toList())

        raii1.close()
        assertEquals(listOf(2,3,6),generateSequence {q.poll()}.toList())

        raii2.close()
        assertEquals(listOf(2,4,8),generateSequence {q.poll()}.toList())
    }
}

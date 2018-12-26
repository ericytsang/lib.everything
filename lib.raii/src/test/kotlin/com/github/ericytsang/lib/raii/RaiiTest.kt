package com.github.ericytsang.lib.raii

import com.github.ericytsang.lib.concurrent.awaitSuspended
import org.junit.Test
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import java.io.Closeable
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread
import kotlin.test.assertEquals
import kotlin.test.assertSame

class RaiiTest
{
    private val closeable1 = mock(Closeable::class.java)
    private val closeable2 = mock(Closeable::class.java)

    @Test
    fun get_returns_instance_provided_in_constructor()
    {
        val raii = Raii<Closeable>(closeable1)
        assertSame(closeable1,raii.obj)
    }

    @Test
    fun get_returns_instance_provided_in_open()
    {
        val raii = Raii<Closeable>(closeable1)
        raii.set {closeable2}
        assertSame(closeable2,raii.obj)
    }

    @Test
    fun open_closes_the_previous_object_before_opening_the_new_one()
    {
        val actions = mutableListOf<Int>()
        val raii = Raii<Closeable>(closeable1)
        raii.set {
            actions.add(1)
            closeable1
        }
        doAnswer {actions.add(2)}.`when`(closeable1).close()
        raii.set {
            actions.add(3)
            closeable1
        }
        assertEquals(listOf(1,2,3),actions)
    }

    @Test
    fun blockingGetNext_returns_once_raii_is_open()
    {
        val raii = Raii<Closeable>(closeable1)
        val q = ArrayBlockingQueue<Closeable>(1)
        val t = thread {
            q.put(raii.blockingGetNext(closeable1))
        }
        t.awaitSuspended()
        raii.set {closeable2}
        t.join()
        assertSame(closeable2,q.take())
    }

    @Test
    fun blockingGetNext_does_not_return_when_raii_open_with_same_obj_and_returns_when_raii_is_open_with_different_obj()
    {
        val raii = Raii<Closeable>(closeable1)
        val q = ArrayBlockingQueue<Closeable>(1)
        val t = thread {
            q.put(raii.blockingGetNext(closeable1))
        }
        t.awaitSuspended()
        raii.set {closeable1}
        t.awaitSuspended()
        check(t.isAlive)
        raii.set {closeable2}
        t.join()
        assertSame(closeable2,q.take())
    }

    @Test
    fun listener_triggered_after_open()
    {
        val q = LinkedBlockingQueue<Int>()
        val raii = Raii(Closeable {q += 0})
        assertEquals(listOf(),generateSequence {q.poll()}.toList())

        raii.listen {q += 2}
        assertEquals(listOf(2),generateSequence {q.poll()}.toList())

        raii.set {
            q += 1
            Closeable {q += 3}
        }
        assertEquals(listOf(2,0,1,2),generateSequence {q.poll()}.toList())
    }

    @Test
    fun listener_triggered_after_open_and_before_close()
    {
        val q = LinkedBlockingQueue<Int>()
        val oldCloseable = Closeable {q += 0}
        val raii = Raii(oldCloseable)
        assertEquals(listOf(),generateSequence {q.poll()}.toList())

        raii.listen {
            q += if (raii.obj != oldCloseable)
            {
                2
            }
            else
            {
                3
            }
            q += if (raii.objOrNullIfClosing != null)
            {
                4
            }
            else
            {
                5
            }
        }
        assertEquals(listOf(3,4),generateSequence {q.poll()}.toList())

        raii.set {
            q += 1
            Closeable {q += 6}
        }
        assertEquals(listOf(3,5,0,1,2,4),generateSequence {q.poll()}.toList())
    }

    @Test
    fun listener_on_multiple_raiis()
    {
        val q = LinkedBlockingQueue<Int>()
        val oldCloseable1 = Closeable {q += 0}
        val oldCloseable2 = Closeable {q += 1}
        val raii1 = Raii(oldCloseable1)
        val raii2 = Raii(oldCloseable2)
        assertEquals(listOf(),generateSequence {q.poll()}.toList())

        listOf(raii1,raii2).listen {
            q += if (raii1.obj != oldCloseable1)
            {
                2
            }
            else
            {
                3
            }
            q += if (raii2.obj != oldCloseable2)
            {
                4
            }
            else
            {
                5
            }
        }
        assertEquals(listOf(3,5,3,5),generateSequence {q.poll()}.toList())

        raii1.set {
            q += 6
            Closeable {q += 7}
        }
        assertEquals(listOf(3,5,0,6,2,5),generateSequence {q.poll()}.toList())

        raii2.set {
            q += 8
            Closeable {q += 9}
        }
        assertEquals(listOf(2,5,1,8,2,4),generateSequence {q.poll()}.toList())
    }
}

package com.github.ericytsang.lib.raii

import com.github.ericytsang.multiwindow.app.android.Raii
import org.junit.Test
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import java.io.Closeable
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
}

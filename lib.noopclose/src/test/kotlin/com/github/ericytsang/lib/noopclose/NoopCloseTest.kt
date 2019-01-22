package com.github.ericytsang.lib.noopclose

import com.github.ericytsang.lib.noopclose.NoopClose
import org.junit.Test
import java.io.Closeable
import kotlin.test.assertEquals

class NoopCloseTest
{
    @Test
    fun the_returned_thing_is_a_closeable()
    {
        val noopClose = NoopClose(8)
        val closeable:Closeable = noopClose
        closeable.close()
        assertEquals(8,noopClose.wrapee)
    }
}

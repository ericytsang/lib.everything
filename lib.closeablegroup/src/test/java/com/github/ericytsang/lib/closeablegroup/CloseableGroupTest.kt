package com.github.ericytsang.lib.closeablegroup

import java.io.Closeable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith

class CloseableGroupTest
{
    private val closeables = CloseableGroup()

    private val thingsThatWereClosed = mutableListOf<Int>()

    private val closeable1 = Closeable {thingsThatWereClosed += 1}
    private val closeable2 = Closeable {thingsThatWereClosed += 2}
    private val closeable3 = Closeable {thingsThatWereClosed += 3}
    private val closeable4 = Closeable {thingsThatWereClosed += 4}

    @Test
    fun close_closes_in_reverse_order()
    {
        // add things to closeable
        closeables + closeable1
        closeables += closeable2
        closeables += {closeable3.close()}
        closeables.add(closeable4)

        // close...
        closeables.close()

        // everything should be closed at this point, in reverse order of adding
        assertEquals(listOf(4,3,2,1),thingsThatWereClosed)
    }

    @Test
    fun should_not_close_closeables_added_to_unclosed_closeable_group()
    {
        // add things to closeable
        closeables + closeable1
        closeables += closeable2
        closeables.add(closeable3)
        closeables += {closeable4.close()}

        // nothing should have been closed at this point
        assertEquals(emptyList<Int>(),thingsThatWereClosed)
    }

    @Test
    fun a_closed_closeable_should_close_subsequently_added_closeables()
    {
        closeables.close()

        // add things to closeable should throw exception
        assertFailsWith(IllegalStateException::class) {closeables+closeable1}
        assertEquals(emptyList<Int>(),thingsThatWereClosed)
    }

    @Test
    fun add_method_returns_the_added_closeable()
    {
        assertEquals(closeable3,closeables.add(closeable3))
    }

    @Test
    fun plus_method_returns_the_added_closeable()
    {
        assertEquals(closeable1,closeables+closeable1)
    }
}
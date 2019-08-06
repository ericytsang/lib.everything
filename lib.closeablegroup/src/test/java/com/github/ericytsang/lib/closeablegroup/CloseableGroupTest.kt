package com.github.ericytsang.lib.closeablegroup

import java.io.Closeable
import kotlin.test.Test
import kotlin.test.assertEquals

class CloseableGroupTest
{
    private val closeables = CloseableGroup()

    private val thingsThatWereClosed = mutableListOf<Int>()

    private val closeable1 = Closeable {thingsThatWereClosed += 1}
    private val closeable2 = Closeable {thingsThatWereClosed += 2}
    private val closeable3 = Closeable {thingsThatWereClosed += 3}

    @Test
    fun close_closes_in_reverse_order()
    {
        // add things to closeable
        closeables.chainedAddCloseables()
        {
            closeables->
            closeables+closeable1
            closeables += closeable2
            closeables += Closeable {closeable3.close()}
        }

        // close...
        closeables.close()

        // everything should be closed at this point, in reverse order of adding
        assertEquals(listOf(3,2,1),thingsThatWereClosed)
    }

    @Test
    fun should_not_close_closeables_added_to_unclosed_closeable_group()
    {
        // add things to closeable
        closeables.chainedAddCloseables()
        {
            closeables->
            closeables + closeable1
            closeables += closeable2
            closeables += Closeable {closeable3.close()}
        }

        // nothing should have been closed at this point
        assertEquals(emptyList<Int>(),thingsThatWereClosed)
    }

    @Test
    fun trying_to_add_things_to_closed_closeable_group_does_nothing()
    {
        closeables.close()
        closeables.chainedAddCloseables {it+closeable1}

        // add things to closed closeable should not do anything
        assertEquals(emptyList<Int>(),thingsThatWereClosed)
    }

    @Test
    fun plus_method_returns_the_added_closeable()
    {
        closeables.chainedAddCloseables()
        {
            assertEquals(closeable1,it+closeable1)
        }
    }
}
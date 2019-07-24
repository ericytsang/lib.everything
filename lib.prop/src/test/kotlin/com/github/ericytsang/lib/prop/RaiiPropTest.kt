package com.github.ericytsang.lib.prop

import com.github.ericytsang.lib.optional.Opt
import org.junit.Test
import java.io.Closeable
import java.util.concurrent.LinkedBlockingQueue
import kotlin.test.assertEquals

class RaiiPropTest
{

    @Test
    fun set_closes_previous_value()
    {
        val events = LinkedBlockingQueue<Int>()
        fun eventsSoFar() = generateSequence {events.poll()}.toList()

        val raiiProp = RaiiProp(Opt.of(Closeable {events += 1}))
        assert(eventsSoFar().isEmpty())

        raiiProp.value = {
            events += 2
            Opt.of(Closeable{events += 3})
        }
        assertEquals(listOf(1,2),eventsSoFar())
    }

    @Test
    fun close_closes_value()
    {
        val events = LinkedBlockingQueue<Int>()
        fun eventsSoFar() = generateSequence {events.poll()}.toList()

        val raiiProp = RaiiProp(Opt.of(Closeable {events += 1}))
        assert(eventsSoFar().isEmpty())

        raiiProp.close()
        assertEquals(listOf(1),eventsSoFar())
    }

    @Test
    fun smoke_test()
    {
        val events = LinkedBlockingQueue<Int>()
        fun eventsSoFar() = generateSequence {events.poll()}.toList()

        val raiiProp = RaiiProp(Opt.of(Closeable {events += 1}))
        assert(eventsSoFar().isEmpty())

        raiiProp.value = {
            events += 2
            Opt.of(Closeable{events += 3})
        }
        assertEquals(listOf(1,2),eventsSoFar())

        raiiProp.value = {
            events += 4
            Opt.of(Closeable{events += 5})
        }
        assertEquals(listOf(3,4),eventsSoFar())

        raiiProp.close()
        assertEquals(listOf(5),eventsSoFar())
    }
}

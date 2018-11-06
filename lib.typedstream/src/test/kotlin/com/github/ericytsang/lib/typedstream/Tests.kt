package com.github.ericytsang.lib.typedstream

import com.github.ericytsang.lib.concurrent.awaitSuspended
import com.github.ericytsang.lib.simplepipestream.SimplePipedInputStream
import com.github.ericytsang.lib.simplepipestream.SimplePipedOutputStream
import com.github.ericytsang.lib.testutils.TestUtils
import com.github.ericytsang.lib.testutils.TestUtils.exceptionExpected
import org.junit.After
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import kotlin.test.assertEquals

class Tests
{
    @After
    fun teardown()
    {
        TestUtils.assertAllWorkerThreadsDead()
    }

    @Test
    fun can_instantiate_input_stream_without_being_blocked_in_the_constructor()
    {
        val stream = TypedInputStream {ObjectInputStream(ByteArrayInputStream(ByteArray(0)))}
        stream.close()
    }

    @Test
    fun returns_all_available_integers_without_blocking()
    {
        val pipedOs = SimplePipedOutputStream(2048)
        val pipedIs = SimplePipedInputStream(pipedOs)
        val typedOs = TypedOutputStream(ObjectOutputStream(pipedOs))
        val typedIs = TypedInputStream {ObjectInputStream(pipedIs)}
        val expected = listOf(1,2,3,4,5)

        expected.forEach {typedOs.send(it)}
        typedOs.close()
        typedIs.reader.awaitSuspended()
        assertEquals(expected,typedIs.readAll(Int::class))

        typedIs.close()
    }

    @Test
    fun reads_in_all_elements_until_queue_is_full()
    {
        val pipedOs = SimplePipedOutputStream(2048)
        val pipedIs = SimplePipedInputStream(pipedOs)
        val typedOs = TypedOutputStream(ObjectOutputStream(pipedOs))
        val typedIs = TypedInputStream(2) {ObjectInputStream(pipedIs)}
        val expected = listOf(1,2,3,4,5)

        expected.forEach {typedOs.send(it)}
        assertEquals(1,typedIs.readOne(Int::class))
        typedIs.reader.awaitSuspended()
        assertEquals(2,typedIs.readOne(Int::class))
        assertEquals(3,typedIs.readOne(Int::class))
        assertEquals(4,typedIs.readOne(Int::class))
        assertEquals(5,typedIs.readOne(Int::class))

        typedOs.close()
        typedIs.close()
    }

    @Test
    fun returns_null_when_stream_is_EOF()
    {
        val typedIs = TypedInputStream {ObjectInputStream(ByteArrayInputStream(ByteArray(0)))}
        typedIs.reader.awaitSuspended()
        assertEquals(null,typedIs.readAll(Int::class))
        typedIs.close()
    }

    @Test
    fun returns_empty_list_when_stream_has_no_new_elements()
    {
        val pipedOs = SimplePipedOutputStream(2048)
        val pipedIs = SimplePipedInputStream(pipedOs)
        val typedOs = TypedOutputStream(ObjectOutputStream(pipedOs))
        val typedIs = TypedInputStream {ObjectInputStream(pipedIs)}

        typedIs.reader.awaitSuspended()
        assertEquals(emptyList(),typedIs.readAll(Int::class))

        typedOs.close()
        typedIs.close()
    }

    @Test
    fun can_instantiate_output_stream_then_close()
    {
        val stream = TypedOutputStream(ObjectOutputStream(ByteArrayOutputStream()))
        stream.close()
    }

    @Test
    fun writing_to_a_closed_output_stream()
    {
        val stream = TypedOutputStream(ObjectOutputStream(ByteArrayOutputStream()))
        stream.close()
        exceptionExpected {
            stream.send(1)
        }
    }

    @Test
    fun writing_to_an_output_stream_with_closed_input_stream()
    {
        val pipedOs = SimplePipedOutputStream(2048)
        val pipedIs = SimplePipedInputStream(pipedOs)
        val typedOs = TypedOutputStream(ObjectOutputStream(pipedOs))
        val typedIs = TypedInputStream {ObjectInputStream(pipedIs)}
        typedIs.close()

        for (i in 1..1000)
        {
            typedOs.send(i)
        }

        exceptionExpected {
            typedOs.close()
        }
    }
}

package com.github.ericytsang.lib.typedstream

import com.github.ericytsang.lib.concurrent.awaitSuspended
import com.github.ericytsang.lib.simplepipestream.SimplePipedInputStream
import com.github.ericytsang.lib.simplepipestream.SimplePipedOutputStream
import com.github.ericytsang.lib.testutils.TestUtils
import org.junit.After
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
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
        val stream = TypedInputStream(Serializable::class,ByteArrayInputStream(ByteArray(0)))
        stream.close()
    }

    @Test
    fun returns_all_available_integers_without_blocking()
    {
        val pipedOs = SimplePipedOutputStream(2048)
        val pipedIs = SimplePipedInputStream(pipedOs)
        val typedOs = TypedOutputStream<Int>(pipedOs)
        val typedIs = TypedInputStream(Int::class,pipedIs)
        val expected = listOf(1,2,3,4,5)

        expected.forEach {typedOs.send(it)}
        typedOs.close()
        typedIs.reader.awaitSuspended()
        assertEquals(expected,typedIs.readAll())

        typedIs.close()
    }

    @Test
    fun returns_null_when_stream_is_EOF()
    {
        val typedIs = TypedInputStream(Int::class,ByteArrayInputStream(ByteArray(0)))
        typedIs.reader.awaitSuspended()
        assertEquals(null,typedIs.readAll())
        typedIs.close()
    }

    @Test
    fun returns_empty_list_when_stream_has_no_new_elements()
    {
        val pipedOs = SimplePipedOutputStream(2048)
        val pipedIs = SimplePipedInputStream(pipedOs)
        val typedOs = TypedOutputStream<Int>(pipedOs)
        val typedIs = TypedInputStream(Int::class,pipedIs)

        typedIs.reader.awaitSuspended()
        assertEquals(emptyList(),typedIs.readAll())

        typedOs.close()
        typedIs.close()
    }

    @Test
    fun can_instantiate_output_stream_then_close()
    {
        val stream = TypedOutputStream<Serializable>(ByteArrayOutputStream())
        stream.close()
    }
}

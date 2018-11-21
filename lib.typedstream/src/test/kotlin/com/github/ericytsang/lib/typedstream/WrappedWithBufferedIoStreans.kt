package com.github.ericytsang.lib.typedstream

import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WrappedWithBufferedIoStreans
{
    @Test
    fun flush_only_called_once_when_it_is_the_last_message_dequeued()
    {
        // initialize
        val unblockAfterVerification = CountDownLatch(1)
        val executorService = Executors.newSingleThreadExecutor()
        {
            thread(start = false)
            {
                unblockAfterVerification.await()
                it.run()
            }
        }
        val buffOsSize = 640000
        val byteOs = ByteArrayOutputStream()
        val streamThatShouldBeFlushed = spy(ObjectOutputStream(BufferedOutputStream(byteOs,buffOsSize)))
        val typeOs = TypedOutputStream(streamThatShouldBeFlushed,executorService)

        // do test
        unblockAfterVerification.countDown()
        for(i in 0..1000)
        {
            typeOs.send(i)
        }
        typeOs.close()

        // verify
        val flushesFromClose = 1
        val flushesFromWorkerThread = 1 // <- this is what we are testing
        verify(streamThatShouldBeFlushed,times(flushesFromClose+flushesFromWorkerThread)).flush()
        assertTrue(byteOs.toByteArray().size < buffOsSize)
    }

    @Test
    fun data_flushed_even_when_only_element_is_smaller_than_the_buffer()
    {
        val buffOsSize = 2048
        val byteOs = ByteArrayOutputStream()
        val streamThatShouldBeFlushed = spy(ObjectOutputStream(BufferedOutputStream(byteOs,buffOsSize)))

        // log every time flush is called
        doAnswer {Throwable().printStackTrace(System.out)}.`when`(streamThatShouldBeFlushed).flush()

        val typeOs = TypedOutputStream(streamThatShouldBeFlushed)

        // run behaviour
        typeOs.send(5)
        typeOs.close()

        // verify
        val flushesFromClose = 1
        val flushesFromWorkerThread = 1 // <- this is what we are testing
        verify(streamThatShouldBeFlushed,times(flushesFromClose+flushesFromWorkerThread)).flush()
        assertTrue(byteOs.toByteArray().size < buffOsSize)
    }
}

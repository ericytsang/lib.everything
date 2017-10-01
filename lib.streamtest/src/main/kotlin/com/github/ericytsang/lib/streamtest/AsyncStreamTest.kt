package com.github.ericytsang.lib.streamtest

import org.junit.After
import org.junit.Test
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.OutputStream
import java.util.Arrays
import java.util.LinkedHashSet
import kotlin.concurrent.thread

/**
 * Created by surpl on 10/29/2016.
 */
abstract class AsyncStreamTest
{
    protected abstract val src:OutputStream
    protected abstract val sink:InputStream
    private var exception:Exception? = null
    private val ts = LinkedHashSet<Thread>()

    @After
    fun teardown()
    {
        ts.forEach {it.join()}
        if (exception != null) throw exception!!
    }

    @Test
    fun pipeByteArrays()
    {
        val written = byteArrayOf(0,2,5,6)
        val read = byteArrayOf(0,0,0,0)
        ts += thread {
            src.write(written)
            src.close()
        }
        ts += thread {DataInputStream(sink).readFully(read)}
        ts.forEach {it.join()}
        assert(Arrays.equals(written,read))
    }

    @Test
    fun pipeNegativeNumber()
    {
        ts += thread {
            try
            {
                src.write(-1)
                src.close()
            }
            catch (ex:Exception)
            {
                exception = ex
            }
        }

        ts += thread {
            try
            {
                assert(sink.read() == 0xFF)
            }
            catch (ex:Exception)
            {
                exception = ex
            }
        }
    }

    @Test
    fun pipeShorts()
    {
        ts += thread {
            try
            {
                DataOutputStream(src).writeShort(0)
                DataOutputStream(src).writeShort(1)
                DataOutputStream(src).writeShort(-1)
                src.close()
            }
            catch (ex:Exception)
            {
                exception = ex
            }
        }

        ts += thread {
            try
            {
                assert(DataInputStream(sink).readShort() == 0.toShort())
                assert(DataInputStream(sink).readShort() == 1.toShort())
                assert(DataInputStream(sink).readShort() == (-1).toShort())
            }
            catch (ex:Exception)
            {
                exception = ex
            }
        }
    }

    @Test
    fun pipeStringObjects()
    {
        ts += thread {
            try
            {
                ObjectOutputStream(src).use {it.writeObject("hello!!!")}
            }
            catch (ex:Exception)
            {
                exception = ex
            }
        }

        ts += thread {
            try
            {
                assert(ObjectInputStream(sink).readObject() == "hello!!!")
            }
            catch (ex:Exception)
            {
                exception = ex
            }
        }
    }

    @Test
    fun pipeMultiFieldObjects()
    {
        ts += thread {
            try
            {
                ObjectOutputStream(src).use {it.writeObject(RuntimeException("blehh"))}
            }
            catch (ex:Exception)
            {
                exception = ex
            }
        }

        ts += thread {
            try
            {
                ObjectInputStream(sink).readObject()
            }
            catch (ex:Exception)
            {
                exception = ex
            }
        }
    }

    @Test
    fun pipeTestWellBeyondEof()
    {
        ts += thread {
            try
            {
                src.write(0)
                src.write(2)
                src.write(5)
                src.write(6)
                src.write(127)
                src.write(128)
                src.write(129)
                src.write(254)
                src.write(255)
                src.close()
            }
            catch (ex:Exception)
            {
                exception = ex
            }
        }

        ts += thread {
            try
            {
                assert(sink.read() == 0)
                assert(sink.read() == 2)
                assert(sink.read() == 5)
                assert(sink.read() == 6)
                assert(sink.read() == 127)
                assert(sink.read() == 128)
                assert(sink.read() == 129)
                assert(sink.read() == 254)
                assert(sink.read() == 255)
                assert(sink.read() == -1)
                assert(sink.read() == -1)
                assert(sink.read() == -1)
                assert(sink.read() == -1)
            }
            catch (ex:Exception)
            {
                exception = ex
            }
        }
    }

    @Test
    fun pipeTestThreadInterrupt()
    {
        ts += thread {
            try
            {
                src.write(0)
                src.write(2)
                src.write(5)
                src.write(6)
                src.write(127)
                src.write(128)
                src.write(129)
                src.write(254)
                src.write(255)
                Thread.sleep(500)
                src.close()
            }
            catch (ex:Exception)
            {
                exception = ex
            }
        }

        ts += thread {
            try
            {
                assert(sink.read() == 0)
                assert(sink.read() == 2)
                assert(sink.read() == 5)
                assert(sink.read() == 6)
                assert(sink.read() == 127)
                assert(sink.read() == 128)
                assert(sink.read() == 129)
                assert(sink.read() == 254)
                assert(sink.read() == 255)
                assert(sink.read() == -1)
                assert(sink.read() == -1)
                assert(sink.read() == -1)
                assert(sink.read() == -1)
            }
            catch (ex:Exception)
            {
                exception = ex
            }
        }
    }
}

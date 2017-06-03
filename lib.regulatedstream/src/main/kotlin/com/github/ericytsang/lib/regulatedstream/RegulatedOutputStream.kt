package com.github.ericytsang.lib.regulatedstream

import com.github.ericytsang.lib.abstractstream.AbstractOutputStream
import java.io.OutputStream
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class RegulatedOutputStream(val stream:OutputStream):AbstractOutputStream()
{
    val permitsMutex = ReentrantLock()

    val permitsChanged:Condition = permitsMutex.newCondition()

    var permits = 0
        private set(value)
        {
            check(permitsMutex.isHeldByCurrentThread)
            field = value
            permitsChanged.signalAll()
        }

    /**
     * allow the output stream to write [numBytes] more bytes in addition to
     * previous calls to permit.
     */
    fun permit(numBytes:Int) = permitsMutex.withLock()
    {
        if (numBytes < 0)
        {
            throw IllegalArgumentException("argument must be > 0: $numBytes")
        }
        while (Int.MAX_VALUE-permits < numBytes)
        {
            permitsChanged.await()
        }
        permits += numBytes
    }

    override fun oneShotClose() = stream.close()

    override fun flush() = stream.flush()

    override fun doWrite(b:ByteArray,off:Int,len:Int) = permitsMutex.withLock()
    {
        var cursor = off
        var remainingLen = len
        while (remainingLen > 0)
        {
            while (permits <= 0)
            {
                permitsChanged.await()
            }
            val bytesToTransmit = Math.min(permits,remainingLen)
            stream.write(b,cursor,bytesToTransmit)
            permits -= bytesToTransmit
            remainingLen -= bytesToTransmit
            cursor += bytesToTransmit
        }
    }
}

package com.github.ericytsang.lib.cipherstream

import com.github.ericytsang.lib.abstractstream.AbstractInputStream
import java.io.DataInputStream
import java.io.EOFException
import java.io.InputStream
import java.nio.ByteBuffer
import java.util.concurrent.locks.ReentrantLock
import javax.crypto.Cipher
import kotlin.concurrent.withLock

class CipherInputStream(_underlyingStream:InputStream,val cipher:Cipher):AbstractInputStream()
{
    private val mutex = ReentrantLock()
    private val underlyingStream = DataInputStream(_underlyingStream)
    private var decodedData = ByteBuffer.wrap(byteArrayOf())
    override fun doRead(b:ByteArray,off:Int,len:Int):Int = mutex.withLock()
    {
        while (!decodedData.hasRemaining())
        {
            val type = try
            {
                underlyingStream.readChar()
            }
            catch (ex:EOFException)
            {
                return -1
            }
            val length = underlyingStream.readInt()
            val readData = ByteArray(length)
            underlyingStream.readFully(readData)
            val rawDecodedData = when (type)
            {
                'c' -> cipher.update(readData) ?: byteArrayOf()
                'f' -> cipher.doFinal(readData) ?: byteArrayOf()
                else -> throw RuntimeException("unhandled type")
            }
            decodedData = ByteBuffer.wrap(rawDecodedData)
        }

        val getLength = Math.min(len,decodedData.remaining())
        decodedData.get(b,off,getLength)
        return getLength
    }

    override fun oneShotClose()
    {
        underlyingStream.close()
    }
}

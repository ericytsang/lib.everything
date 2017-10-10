package com.github.ericytsang.lib.cipherstream

import com.github.ericytsang.lib.abstractstream.AbstractOutputStream
import java.io.DataOutputStream
import java.io.OutputStream
import javax.crypto.Cipher

class CipherOutputStream(_underlyingStream:OutputStream,val cipher:Cipher):AbstractOutputStream()
{
    private val underlyingStream = DataOutputStream(_underlyingStream)

    override fun doWrite(b:ByteArray,off:Int,len:Int) = synchronized(underlyingStream)
    {
        val data = cipher.update(b,off,len) ?: return _flush()
        underlyingStream.writeChar('c'.toInt())
        underlyingStream.writeInt(data.size)
        underlyingStream.write(data)
        _flush()
    }

    private fun _flush() = synchronized(underlyingStream)
    {
        val data = cipher.doFinal() ?: return
        underlyingStream.writeChar('f'.toInt())
        underlyingStream.writeInt(data.size)
        underlyingStream.write(data)
    }

    override fun flush() = synchronized(underlyingStream)
    {
        underlyingStream.flush()
    }

    override fun oneShotClose()
    {
        try
        {
            flush()
        }
        catch (ex:Exception)
        {
            // ignore it....we're closing the stream anyway
        }
        underlyingStream.close()
    }
}

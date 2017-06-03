package com.github.ericytsang.lib.simplepipestream

import com.github.ericytsang.lib.cipherstream.CipherInputStream
import com.github.ericytsang.lib.cipherstream.CipherOutputStream
import com.github.ericytsang.lib.streamtest.AsyncStreamTest
import java.io.InputStream
import java.io.OutputStream
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class AsyncStreamTest:AsyncStreamTest()
{
    override val sink:InputStream
    override val src:OutputStream

    val pipedO = SimplePipedOutputStream()
    val pipedI = SimplePipedInputStream(pipedO)

    init
    {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec("1234567890123456".toByteArray(),"AES"),
            IvParameterSpec("1234567890123456".toByteArray()))
        src = CipherOutputStream(pipedO,cipher)
    }

    init
    {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec("1234567890123456".toByteArray(),"AES"),
            IvParameterSpec("1234567890123456".toByteArray()))
        sink = CipherInputStream(pipedI,cipher)
    }
}

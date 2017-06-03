package com.github.ericytsang.lib.simplepipestream

import org.junit.Test
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.crypto.Cipher

class EncryptTest
{
    val iv = IvParameterSpec("Bar12345Bar12345".toByteArray(charset("UTF-8")))
    val skeySpec = SecretKeySpec("RandomInitVector".toByteArray(charset("UTF-8")),"AES")
    val encryptingCipher = run()
    {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
        cipher.init(Cipher.ENCRYPT_MODE,skeySpec,iv)
        cipher
    }
    val decryptingCipher = run()
    {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
        cipher.init(Cipher.DECRYPT_MODE,skeySpec,iv)
        cipher
    }

    @Test
    fun main()
    {
        encryptingCipher.update("1234567890123456".toByteArray())
            .let {print(decryptingCipher.update(it ?: return@let)?.let{String(it)}?:"")}
        encryptingCipher.update("12345673456\n".toByteArray())
            .let {print(decryptingCipher.update(it ?: return@let)?.let{String(it)}?:"")}
        encryptingCipher.doFinal()
            .let {print(decryptingCipher.doFinal(it ?: return@let)?.let{String(it)}?:"")}
        println("====================")
        encryptingCipher.update("1234567890123456".toByteArray())
            .let {print(decryptingCipher.update(it ?: return@let)?.let{String(it)}?:"")}
        encryptingCipher.update("1234567890123456".toByteArray())
            .let {print(decryptingCipher.update(it ?: return@let)?.let{String(it)}?:"")}
        encryptingCipher.update("123456786\n".toByteArray())
            .let {print(decryptingCipher.update(it ?: return@let)?.let{String(it)}?:"")}
        encryptingCipher.doFinal()
            .let {print(decryptingCipher.doFinal(it ?: return@let)?.let{String(it)}?:"")}
    }
}

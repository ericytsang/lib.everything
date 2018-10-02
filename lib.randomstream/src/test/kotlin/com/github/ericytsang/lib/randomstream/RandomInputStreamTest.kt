package com.github.ericytsang.lib.randomstream

import org.junit.Test
import java.io.DataInputStream
import java.util.*
import kotlin.concurrent.thread

class RandomInputStreamTest
{
    val randomInputStream = RandomInputStream()

    @Test
    fun generates_all_combinations_of_bits()
    {
        val allBytes = Collections.synchronizedSet((Byte.MIN_VALUE..Byte.MAX_VALUE).toMutableSet())
        val ba = byteArrayOf(0)
        (1..4)
                .map {
                    thread {
                        while(allBytes.isNotEmpty())
                        {
                            randomInputStream.read(ba)
                            allBytes.remove(ba[0].toInt())
                            println(allBytes)
                        }
                    }
                }
                .forEach {it.join()}
    }

    @Test
    fun can_be_used_with_data_input_stream()
    {
        println(DataInputStream(randomInputStream).readLong())
        println(DataInputStream(randomInputStream).readLong())
        println(DataInputStream(randomInputStream).readLong())
        println(DataInputStream(randomInputStream).readLong())
        println(DataInputStream(randomInputStream).readLong())
        println(DataInputStream(randomInputStream).readLong())
        println(DataInputStream(randomInputStream).readLong())
    }
}

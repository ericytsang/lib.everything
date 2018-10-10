package com.github.ericytsang.lib.typedstream

import java.io.Closeable
import java.io.InputStream
import java.io.ObjectInputStream
import java.io.Serializable
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock
import kotlin.reflect.KClass
import kotlin.reflect.full.cast

class TypedInputStream<Message:Serializable>(
        val messageClass:KClass<Message>,
        underlyingStream:InputStream,
        backlogSize:Int = 10)
    :Closeable
{
    private val instantiationSite = listOf(1,2)
            .map {StacktraceIndex(it)}
            .map {getFileNameAndLine(it)}
            .find {this::class.simpleName!! !in it}!!
            .toString()
    private val readLock = ReentrantLock()
    private val objectIs by lazy {ObjectInputStream(underlyingStream)}
    private val backlog = ArrayBlockingQueue<Poisonous<Message>>(backlogSize)
    internal val reader by lazy()
    {
        thread(name = getFileNameAndLine(StacktraceIndex()))
        {
            while (!closed)
            {
                try
                {
                    val serialized = objectIs.readObject()
                    val message = messageClass.cast(serialized)
                    backlog.put(Poisonous.Data(message))
                }
                catch (e:Throwable)
                {
                    if (!closed)
                    {
                        Exception("${this::class.simpleName} instantiated at $instantiationSite says: stream closed by remote",e)
                                .printStackTrace(System.out)
                    }
                    break
                }
            }
            backlog.put(Poisonous.Poison())
        }
    }

    /**
     * returns a list of all new received messages in the order that they were
     * received since the last call to [readAll] or [readOne], or null if there
     * are no more messages because the stream is closed either by a call to
     * [close], or by EOF.
     */
    fun readAll():List<Message>? = readLock.withLock()
    {
        reader
        val newMessages = generateSequence {backlog.poll()}.toList()
        val poison = newMessages.filterIsInstance<Poisonous.Poison<Message>>()
        val data = newMessages.filterIsInstance<Poisonous.Data<Message>>()

        // put all poison back into the backlog for subsequent calls to read...
        poison.forEach {backlog.put(it)}

        // return null if there was only poison left in the backlog;
        // return data otherwise
        return if (data.isEmpty() && poison.isNotEmpty())
        {
            null
        }
        else
        {
            data.map {it.payload}
        }
    }

    /**
     * returns the next message received since the last call to [readAll] or
     * [readOne], blocking if necessary, or null if there are no more messages
     * because the stream is closed either by a call to [close], or by EOF.
     */
    fun readOne():Message? = readLock.withLock()
    {
        reader
        val newMessage = backlog.take()
        return when(newMessage)
        {
            is Poisonous.Poison ->
            {
                // put poison back into the backlog for subsequent calls to read...
                backlog.put(newMessage)
                null
            }
            is Poisonous.Data ->
            {
                newMessage.payload
            }
        }
    }

    private var closed = false
    override fun close()
    {
        closed = true
        try
        {
            objectIs.close()
        }
        catch (e:Throwable)
        {
            // ignore
        }
        reader.join()
    }

    private sealed class Poisonous<Payload:Any>
    {
        class Poison<Payload:Any>:Poisonous<Payload>()
        class Data<Payload:Any>(val payload:Payload):Poisonous<Payload>()
    }
}

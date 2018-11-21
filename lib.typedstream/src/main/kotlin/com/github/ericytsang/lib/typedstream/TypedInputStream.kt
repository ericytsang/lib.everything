package com.github.ericytsang.lib.typedstream

import com.github.ericytsang.lib.getfileandline.StacktraceIndex
import com.github.ericytsang.lib.getfileandline.getFileNameAndLine
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

class TypedInputStream(
        backlogSize:Int = 10,
        underlyingStream:()->ObjectInputStream)
    :Closeable
{
    private val instantiationSite = (0..3)
            .map {StacktraceIndex(it)}
            .mapNotNull {getFileNameAndLine(it)}
            .find {this::class.simpleName!! !in it}!!
            .toString()
    private val instantiationStackTrace = Throwable()
    private val readLock = ReentrantLock()
    private val objectIs by lazy {underlyingStream()}
    private val backlog = ArrayBlockingQueue<Poisonous>(backlogSize)
    internal val reader by lazy()
    {
        thread(name = getFileNameAndLine(StacktraceIndex()))
        {
            while (!closed)
            {
                try
                {
                    val serialized = objectIs.readObject()
                    serialized as Serializable
                    backlog.put(Poisonous.Data(serialized))
                }
                catch (e:Throwable)
                {
                    if (!closed)
                    {
                        Exception("${this::class.simpleName} instantiated at $instantiationSite says: stream closed by remote",e)
                                .printStackTrace(System.out)
                        print("Instantiation site of ${this::class.simpleName}: ")
                        instantiationStackTrace.printStackTrace(System.out)
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
    fun <Message:Any> readAll(messageClass:KClass<Message>):List<Message>? = readLock.withLock()
    {
        reader // initialize the lazy value
        val newMessages = generateSequence {backlog.poll()}.toList()
        val poison = newMessages.filterIsInstance<Poisonous.Poison>()
        val data = newMessages.filterIsInstance<Poisonous.Data>()

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
            data.asSequence()
                    .map {it.payload}
                    .map {messageClass.cast(it)}
                    .toList()
        }
    }

    /**
     * returns the next message received since the last call to [readAll] or
     * [readOne], blocking if necessary, or null if there are no more messages
     * because the stream is closed either by a call to [close], or by EOF.
     */
    fun <Message:Any> readOne(messageClass:KClass<Message>):Message? = readLock.withLock()
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
                messageClass.cast(newMessage.payload)
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

    private sealed class Poisonous
    {
        class Poison:Poisonous()
        class Data(val payload:Serializable):Poisonous()
    }
}

package com.github.ericytsang.lib.typedstream

import com.github.ericytsang.lib.getfileandline.StacktraceIndex
import com.github.ericytsang.lib.getfileandline.getFileNameAndLine
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.ObjectOutputStream
import java.io.PrintStream
import java.io.PrintWriter
import java.io.Serializable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class TypedOutputStream(
        private val objectOs:ObjectOutputStream,
        private val sender:ExecutorService = makeExecutor())
    :Closeable
{
    companion object
    {
        fun makeExecutor():ExecutorService
        {
            val instantiationSite = Throwable("thread instantiation site")
            val byteOs = ByteArrayOutputStream()
            instantiationSite.printStackTrace(PrintWriter(byteOs))
            return Executors.newSingleThreadExecutor {
                thread(start = false,name = String(byteOs.toByteArray())) {it.run()}
            }
        }
    }

    private var latestSubmittedMessageId = Int.MAX_VALUE

    fun send(message:Serializable)
    {
        val messageId = ++latestSubmittedMessageId
        sender.submit()
        {
            objectOs.writeObject(message)
            if (latestSubmittedMessageId == messageId)
            {
                objectOs.flush()
            }
        }
    }

    override fun close()
    {
        // stop taking send requests
        sender.shutdown()

        // finish sending the last thing
        check(sender.awaitTermination(5,TimeUnit.SECONDS))

        // close the stream
        objectOs.close()
    }
}

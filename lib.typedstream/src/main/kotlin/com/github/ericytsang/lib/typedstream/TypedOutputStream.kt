package com.github.ericytsang.lib.typedstream

import java.io.Closeable
import java.io.ObjectOutputStream
import java.io.OutputStream
import java.io.Serializable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class TypedOutputStream(
        private val objectOs:ObjectOutputStream)
    :Closeable
{
    private val instantiationSite = getFileNameAndLine(StacktraceIndex(1))
    private val sender = Executors.newSingleThreadExecutor {
        thread(start = false,name = "$instantiationSite -> ${getFileNameAndLine(StacktraceIndex())}") {it.run()}
    }

    fun send(message:Serializable)
    {
        sender.submit {objectOs.writeObject(message)}
    }

    override fun close()
    {
        // stop taking send requests
        sender.shutdown()

        // finish sending the last thing
        check(sender.awaitTermination(5,TimeUnit.SECONDS))
        objectOs.flush()

        // close the stream
        objectOs.close()
    }
}

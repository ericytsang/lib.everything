package com.github.ericytsang.lib.onlycallonce

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

sealed class OneshotCall<in Input>
{
    companion object
    {
        fun <Input> builder() = Builder1<Input>()

        class Builder1<Input> internal constructor()
        {
            private var wrap:(OneshotCall<Input>)->OneshotCall<Input> = {it}

            fun serialized():Builder1<Input>
            {
                wrap = {Serialized(it)}
                return this
            }

            fun ignoreSubsequent(callHandler:(Input)->Unit):OneshotCall<Input>
            {
                return wrap(IgnoreSubsequentCalls(callHandler))
            }

            fun throwSubsequent(callHandler:(Input)->Unit):OneshotCall<Input>
            {
                return wrap(ThrowOnSubsequentCalls(callHandler))
            }
        }
    }

    abstract fun call(input:Input)
    abstract val callRecord:CallRecord?
    val isVirgin:Boolean get() = callRecord == null

    class CallRecord(val callingThread:Thread):Exception("oneshot call already used. first call by thread: $callingThread")

    class Serialized<in Input>(val wrappee:OneshotCall<Input>):OneshotCall<Input>()
    {
        private val callLock = ReentrantLock()
        override val callRecord:CallRecord? get() = wrappee.callRecord
        override fun call(input:Input)
        {
            callLock.withLock {wrappee.call(input)}
        }
    }

    class IgnoreSubsequentCalls<in Input>(val callHandler:(Input)->Unit):OneshotCall<Input>()
    {
        private val wrappee = ThrowOnSubsequentCalls(callHandler)
        override fun call(input:Input)
        {
            try
            {
                wrappee.call(input)
            }
            catch (ex:ThrowOnSubsequentCalls.AlreadyCalledException)
            {
                // ignore
            }
        }
        override val callRecord:CallRecord? get() = wrappee.callRecord
    }

    class ThrowOnSubsequentCalls<in Input>(val callHandler:(Input)->Unit):OneshotCall<Input>()
    {
        override var callRecord:CallRecord? = null
            private set
            get() = callLock.withLock {field}
        private val callLock = ReentrantLock()
        override fun call(input:Input)
        {
            callLock.withLock()
            {
                val callRecord = callRecord
                if (callRecord == null)
                {
                    this.callRecord = CallRecord(Thread.currentThread())
                }
                else
                {
                    throw AlreadyCalledException(callRecord)
                }
            }
            return callHandler(input)
        }
        class AlreadyCalledException(cause:CallRecord):Exception(cause)
    }
}


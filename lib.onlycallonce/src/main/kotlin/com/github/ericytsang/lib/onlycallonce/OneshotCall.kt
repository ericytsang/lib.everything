package com.github.ericytsang.lib.onlycallonce

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

sealed class OneshotCall<in Input>
{
    companion object
    {
        fun <Input> builder() = Builder<Input>()

        class Builder<Input>
        {
            private var wrap:(OneshotCall<Input>)->OneshotCall<Input> = {it}

            fun serialized():Builder<Input>
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

    class Serialized<in Input>(val wrappee:OneshotCall<Input>):OneshotCall<Input>()
    {
        private val callLock = ReentrantLock()
        override fun call(input:Input)
        {
            callLock.withLock {wrappee.call(input)}
        }
    }

    class IgnoreSubsequentCalls<in Input>(val callHandler:(Input)->Unit):OneshotCall<Input>()
    {
        private var isFirstCall = true
        private val callLock = ReentrantLock()
        override fun call(input:Input)
        {
            val isFirstCall = callLock.withLock()
            {
                val isFirstCall = isFirstCall
                if (isFirstCall)
                {
                    this.isFirstCall = false
                }
                isFirstCall
            }
            if (isFirstCall)
            {
                callHandler(input)
            }
        }
    }

    class ThrowOnSubsequentCalls<in Input>(val callHandler:(Input)->Unit):OneshotCall<Input>()
    {
        private var stacktraceOfFirstCall:Array<StackTraceElement>? = null
        private val callLock = ReentrantLock()
        override fun call(input:Input)
        {
            callLock.withLock()
            {
                if (stacktraceOfFirstCall == null)
                {
                    stacktraceOfFirstCall = Thread.currentThread().stackTrace
                }
                else
                {
                    throw Exception()
                }
            }
            return callHandler(input)
        }
        class Exception:IllegalStateException()
    }
}


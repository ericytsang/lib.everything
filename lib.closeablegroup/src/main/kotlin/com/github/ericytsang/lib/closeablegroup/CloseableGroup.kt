package com.github.ericytsang.lib.closeablegroup

import com.github.ericytsang.lib.closeablegroup.CloseableGroup.State.Active
import com.github.ericytsang.lib.closeablegroup.CloseableGroup.State.Closed
import java.io.Closeable
import java.lang.IllegalStateException
import java.util.Stack
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class CloseableGroup(
        vararg _closeables:Closeable)
    :Closeable
{
    private val lock = ReentrantLock()
    private var state:State = Active(*_closeables)

    private fun addCloseable(closeable:Closeable) = lock.withLock()
    {
        state.addCloseable(closeable)
    }

    override fun close() = lock.withLock()
    {
        state.close()
        state = when(state)
        {
            is Active -> Closed()
            is Closed -> state
        }
    }

    fun <TCloseable:Closeable> add(other:TCloseable):TCloseable
    {
        addCloseable(other)
        return other
    }

    operator fun <TCloseable:Closeable> plus(other:TCloseable):TCloseable
    {
        addCloseable(other)
        return other
    }

    operator fun plusAssign(other:Closeable)
    {
        addCloseable(other)
    }

    operator fun <R> plusAssign(other:()->R)
    {
        addCloseable(Closeable {other()})
    }

    sealed class State
    {
        abstract fun addCloseable(closeable:Closeable)
        abstract fun close()

        class Active(
                vararg _closeables:Closeable)
            :State()
        {
            private val closeables = Stack<Closeable>().apply {addAll(_closeables)}
            override fun addCloseable(closeable:Closeable) = Unit.also {closeables.push(closeable)}
            override fun close() = generateSequence {closeables.runCatching {pop()}.getOrNull()}.forEach {it.close()}
        }

        class Closed:State()
        {
            private val wasAlreadyClosedHere = IllegalStateException("was already closed here.")
            override fun addCloseable(closeable:Closeable) = throw wasAlreadyClosedHere
            override fun close() = Unit
        }
    }
}

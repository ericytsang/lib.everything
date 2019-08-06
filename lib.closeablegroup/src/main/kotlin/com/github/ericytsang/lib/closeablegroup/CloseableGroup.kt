package com.github.ericytsang.lib.closeablegroup

import com.github.ericytsang.lib.closeablegroup.CloseableGroup.State.Active
import com.github.ericytsang.lib.closeablegroup.CloseableGroup.State.Closed
import java.io.Closeable
import java.util.Stack
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class CloseableGroup(
        vararg _closeables:Closeable)
    :Closeable
{
    fun chainedAddCloseables(block:(scope:AddCloseablesScope)->Unit):CloseableGroup = apply {coreApi.addCloseables(block)}
    fun <R:Any> addCloseables(block:(scope:AddCloseablesScope)->R):R? = coreApi.addCloseables(block)
    override fun close() = coreApi.close()

    interface AddCloseablesScope
    {
        operator fun <TCloseable:Closeable> plus(other:TCloseable):TCloseable
        operator fun <TCloseable:Closeable> plusAssign(other:TCloseable)
    }

    private val coreApi = object
    {
        private val lock = ReentrantLock()
        private var state:State = Active(*_closeables)

        fun <R:Any> addCloseables(block:(scope:AddCloseablesScope)->R):R? = lock.withLock()
        {
            state.addCloseables()
            {
                scope->
                block(object:AddCloseablesScope
                {
                    override fun <TCloseable:Closeable> plus(other:TCloseable):TCloseable = other.also {scope.add(other)}
                    override fun <TCloseable:Closeable> plusAssign(other:TCloseable) = Unit.also {scope.add(other)}
                })
            }
        }

        fun close() = lock.withLock()
        {
            state.close()
            state = when(state)
            {
                is Active -> Closed()
                is Closed -> state
            }
        }
    }

    private sealed class State
    {
        abstract fun <R:Any> addCloseables(block:(scope:AddCloseablesScope)->R):R?
        abstract fun close()

        interface AddCloseablesScope
        {
            fun <TCloseable:Closeable> add(other:TCloseable):TCloseable
        }

        class Active(
                vararg _closeables:Closeable)
            :State()
        {
            private val closeables = Stack<Closeable>().apply {addAll(_closeables)}
            override fun close() = generateSequence {closeables.runCatching {pop()}.getOrNull()}.forEach {it.close()}
            override fun <R:Any> addCloseables(block:(scope:AddCloseablesScope)->R):R
            {
                return block(object:AddCloseablesScope
                {
                    override fun <TCloseable:Closeable> add(other:TCloseable) = other.also {closeables += other}
                })
            }
        }

        class Closed:State()
        {
            override fun <R:Any> addCloseables(block:(scope:AddCloseablesScope)->R):R? = null
            override fun close() = Unit
        }
    }
}

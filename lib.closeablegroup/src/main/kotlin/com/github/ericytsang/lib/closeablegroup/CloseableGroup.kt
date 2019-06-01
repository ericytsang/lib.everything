package com.github.ericytsang.lib.closeablegroup

import java.io.Closeable
import java.util.Stack

class CloseableGroup(
        vararg _closeables:Closeable)
    :Closeable
{
    private val closeables = Stack<Closeable>()
            .apply {addAll(_closeables)}

    operator fun plusAssign(other:Closeable)
    {
        closeables += other
    }

    operator fun <R> plusAssign(other:()->R)
    {
        closeables += Closeable {other()}
    }

    override fun close()
    {
        generateSequence {closeables.runCatching {pop()}.getOrNull()}
                .forEach {it.close()}
    }
}

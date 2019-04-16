package com.github.ericytsang.lib.closeablegroup

import java.io.Closeable

class CloseableGroup(
        vararg _closeables:Closeable)
    :Closeable
{
    private val closeables = mutableListOf(*_closeables)

    operator fun plusAssign(other:Closeable)
    {
        closeables += other
    }

    override fun close()
    {
        closeables.toList().asReversed().forEach {it.close()}
    }
}

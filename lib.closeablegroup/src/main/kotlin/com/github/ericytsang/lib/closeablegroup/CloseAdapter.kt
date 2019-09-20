package com.github.ericytsang.lib.closeablegroup

import java.io.Closeable

data class CloseAdapter<T>(
        val wrapped:T,
        val close:T.()->Unit
):Closeable
{
    override fun close() = wrapped.close()
}


package com.github.ericytsang.lib.awaitable

import java.io.Closeable

interface Awaiter:Closeable
{
    fun onSignal(updateStamp:Long)
}

package com.github.ericytsang.lib.optional

import java.io.Closeable

class OptCloser<Wrapped:Closeable>(
        val opt:Opt<Wrapped>)
    :Closeable
{
    override fun close()
    {
        when(opt)
        {
            is Opt.Some -> opt.opt.close()
            is Opt.None -> Unit
        }
    }
}

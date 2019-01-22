package com.github.ericytsang.lib.noopclose

import java.io.Closeable

class NoopClose<Wrapee:Any>(val wrapee:Wrapee):Closeable
{
    override fun close() = Unit
}

package com.github.ericytsang.lib.cipherstream

import com.github.ericytsang.lib.streamtest.AsyncStreamTest

class AsyncSimplePipeStreamTest:AsyncStreamTest()
{
    override val src = SimplePipedOutputStream(5)
    override val sink = SimplePipedInputStream(src)
}

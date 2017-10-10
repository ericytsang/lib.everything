package com.github.ericytsang.lib.cipherstream

import com.github.ericytsang.lib.streamtest.StreamTest

class SimplePipeStreamTest:StreamTest()
{
    override val src = SimplePipedOutputStream(65536)
    override val sink = SimplePipedInputStream(src)
}

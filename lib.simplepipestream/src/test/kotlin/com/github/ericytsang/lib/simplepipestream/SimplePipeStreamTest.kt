package com.github.ericytsang.lib.simplepipestream

import com.github.ericytsang.lib.streamtest.StreamTest

class SimplePipeStreamTest:StreamTest()
{
    override val src = SimplePipedOutputStream(1024)
    override val sink = SimplePipedInputStream(src)
}

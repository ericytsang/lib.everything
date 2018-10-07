package com.github.ericytsang.lib.streamtest

import java.io.InputStream
import java.io.OutputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream

class StreamTestTest:StreamTest()
{
    override val src:OutputStream = PipedOutputStream()
    override val sink:InputStream = PipedInputStream(src as PipedOutputStream,5)
}

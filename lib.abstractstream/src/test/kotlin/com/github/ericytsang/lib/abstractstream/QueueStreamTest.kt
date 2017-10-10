package com.github.ericytsang.lib.abstractstream

import com.github.ericytsang.lib.streamtest.StreamTest

class QueueStreamTest:StreamTest()
{
    override val src:QueueOutputStream = QueueOutputStream()
    override val sink:QueueInputStream = QueueInputStream(src)
}

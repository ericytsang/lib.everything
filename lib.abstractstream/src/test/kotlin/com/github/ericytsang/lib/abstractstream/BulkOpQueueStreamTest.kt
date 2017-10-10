package com.github.ericytsang.lib.abstractstream

import com.github.ericytsang.lib.streamtest.StreamTest

class BulkOpQueueStreamTest:StreamTest()
{
    override val src:BulkOpQueueOutputStream = BulkOpQueueOutputStream()
    override val sink:BulkOpQueueInputStream = BulkOpQueueInputStream(src)
}

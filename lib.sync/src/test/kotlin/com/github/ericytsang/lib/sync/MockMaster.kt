package com.github.ericytsang.lib.sync

open class MockMaster:Master<Request>
{
    override fun requestSnapshot() = Unit
    override fun process(requests:List<Request>) = Unit
    override fun close() = Unit
}

package com.github.ericytsang.lib.sync

open class MockMaster:Master<Event,Request>
{
    override fun getPendingEvents():List<Event>? = emptyList()
    override fun generateSnapshot():List<Event>? = emptyList()
    override fun process(requests:List<Request>) = Unit
    override fun close() = Unit
}

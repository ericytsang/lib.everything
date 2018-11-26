package com.github.ericytsang.lib.sync

open class MockMaster:Master<Event,Request>
{
    override fun getPendingEvents():List<Event> = emptyList()
    override fun generateSnapshot():List<Event> = emptyList()
    override fun apply(events:List<Event>) = Unit
    override fun getPendingRequests():List<Request>? = emptyList()
    override fun process(requests:List<Request>) = Unit
}

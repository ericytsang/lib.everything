package com.github.ericytsang.lib.sync

open class MockSlave:Slave<Event,Request>
{
    override fun apply(events:List<Event>) = Unit
    override fun getPendingRequests():List<Request>? = emptyList()
}

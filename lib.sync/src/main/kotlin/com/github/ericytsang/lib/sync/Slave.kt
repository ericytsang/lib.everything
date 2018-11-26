package com.github.ericytsang.lib.sync

interface Slave<Event,Request>
{
    /**
     * updates the state of this [Slave] from the [events].
     */
    fun apply(events:List<Event>)

    /**
     * returns all the pending [Request] objects received from this [Slave].
     * may return null if the [Slave] has disconnected.
     */
    fun getPendingRequests():List<Request>?
}

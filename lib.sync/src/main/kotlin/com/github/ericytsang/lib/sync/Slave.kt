package com.github.ericytsang.lib.sync

import java.io.Closeable

interface Slave<Event,Request>:Closeable
{
    /**
     * updates the state of this [Slave] from the [events].
     */
    fun apply(events:List<Event>)

    /**
     * returns all the pending [Request] objects received from this [Slave].
     * may return null if the [Slave] has disconnected.
     * should block until [Request]s become available.
     */
    fun getPendingRequests():List<Request>?

    /**
     * kindly asks [Slave] to shut down, and return null from
     * [getPendingRequests] in the near future.
     */
    override fun close()
}

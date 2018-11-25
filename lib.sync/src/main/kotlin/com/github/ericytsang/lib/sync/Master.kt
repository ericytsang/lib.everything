package com.github.ericytsang.lib.sync

import java.io.Closeable

interface Master<Request>:Closeable
{
    /**
     * asks [Master] to call [Synchronizer.broadcastSnapshot] in the near
     * future with events to bring new clients up to speed.
     */
    fun requestSnapshot()

    /**
     * process [requests] which have been received from the [Slave]s.
     */
    fun process(requests:List<Request>)

    /**
     * called when [Synchronizer.close] is called. when this is called, all
     * blocked and future calls to [getPendingEvents] and [generateSnapshot]
     * should immediately return null.
     */
    override fun close()
}

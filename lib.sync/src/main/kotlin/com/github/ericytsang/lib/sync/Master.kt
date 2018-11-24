package com.github.ericytsang.lib.sync

import java.io.Closeable

interface Master<Event,Request>:Closeable
{
    /**
     * returns the [Event] objects to broadcast to other [Slave] objects.
     * returns null upon EOF (no more [Event] objects).
     * should block until [Event]s become available.
     */
    fun getPendingEvents():List<Event>?

    /**
     * returns the [Event] objects to broadcast to newly added [Slave] objects
     * to bring them up to speed.
     */
    fun generateSnapshot():List<Event>?

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

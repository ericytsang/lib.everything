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
     * process all the requests received from the [Slave]s.
     */
    fun process(requests:List<Request>)

    /**
     * kindly asks [Master] to shut down, and return null from
     * [getPendingEvents] in the near future.
     */
    override fun close()
}

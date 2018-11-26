package com.github.ericytsang.lib.sync

interface Master<Event,Request>:Slave<Event,Request>
{
    /**
     * returns [Event]s to be broadcasted and applied ([apply]) to all connected
     * [Slave]s.
     */
    fun getPendingEvents():List<Event>

    /**
     * asks [Master] to call [Synchronizer.broadcastSnapshot] in the near
     * future with events to bring new clients up to speed.
     */
    fun generateSnapshot():List<Event>

    /**
     * process [requests] which have been received from the [Slave]s.
     */
    fun process(requests:List<Request>)
}

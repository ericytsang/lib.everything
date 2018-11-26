package com.github.ericytsang.lib.sync

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class Synchronizer<Event,Request>(
        private val master:Master<Event,Request>,
        private val listener:Listener)
{
    private val cycleLock = ReentrantLock()
    private val virginSlaves = mutableSetOf<Slave<Event,Request>>()
    private val slaves = mutableSetOf<Slave<Event,Request>>()

    fun cycle():Unit = cycleLock.withLock()
    {
        // send snapshots to all virgin slaves
        if (virginSlaves.isNotEmpty())
        {
            val snapshotEvents = master.generateSnapshot()
            virginSlaves.forEach {it.apply(snapshotEvents)}
            virginSlaves.clear()
        }

        // get events and apply them to all realities
        val broadcastEvents = master.getPendingEvents()
        (slaves+master).forEach {it.apply(broadcastEvents)}

        // process all requests from slaves, and remove disconnected slaves
        val (disconnectedSlaves,slavesWithRequests) = (slaves+master)
                .map {it to it.getPendingRequests()}
                .partition {it.second == null}
        slavesWithRequests
                .map {it.second!!}
                .forEach {master.process(it)}
        slaves.removeAll(disconnectedSlaves.map {it.first})
    }

    fun add(slave:Slave<Event,Request>):Unit = cycleLock.withLock()
    {
        virginSlaves += slave
        slaves += slave
        listener.onSlaveAdded()
    }

    fun rm(slave:Slave<Event,Request>):Unit = cycleLock.withLock()
    {
        virginSlaves -= slave
        slaves -= slave
    }

    interface Listener
    {
        fun onSlaveAdded()
    }
}

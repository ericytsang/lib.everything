package com.github.ericytsang.lib.sync

import java.io.Closeable
import java.util.concurrent.ThreadFactory
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class Synchronizer<Event,Message>(
        master:Master<Event,Message>,
        threadFactory:ThreadFactory)
    :
        Closeable
{
    private var state:State<Event,Message> = Opened(master,threadFactory)
    private val stateAccess = ReentrantLock()
    fun add(slave:Slave<Event,Message>) = stateAccess.withLock {state.add(slave)}
    fun rm(slave:Slave<Event,Message>) = stateAccess.withLock {state.rm(slave)}
    override fun close() = stateAccess.withLock {state.close()}

    data class SlaveEntry<Event,Message>(
            private val slave:Slave<Event,Message>,
            val threadFactory:ThreadFactory,
            val master:Master<Event,Message>)
        :
            Closeable,
            Slave<Event,Message> by slave
    {
        val worker = threadFactory.newThread()
        {
            do
            {
                val messages = slave.getPendingRequests()
                if (!isClosed)
                {
                    master.process(messages ?: emptyList())
                }
            }
            while (!isClosed && messages != null)
            val hostIdentifier = if (isClosed) "local" else "remote"
            Throwable("worker ending. closed by $hostIdentifier host").printStackTrace(System.out)
        }

        init
        {
            worker.name = ::worker.name
        }

        override fun hashCode() = slave.hashCode()
        override fun equals(other:Any?) = slave == other

        var isClosed = false
        override fun close()
        {
            Throwable("close() called by local host").printStackTrace(System.out)
            isClosed = true
            slave.close()
            worker.join()
        }
    }

    private interface State<Event,Message>
    {
        fun add(slave:Slave<Event,Message>)
        fun rm(slave:Slave<Event,Message>)
        fun close()
    }

    private inner class Opened<Event,Message>(
            val master:Master<Event,Message>,
            val threadFactory:ThreadFactory)
        :
            State<Event,Message>
    {
        private val slaves = mutableMapOf<Slave<Event,Message>,SlaveEntry<Event,Message>>()
        private val slavesAccess = ReentrantLock()

        private val masterWorker = threadFactory.newThread()
        {
            do
            {
                val events = master.getPendingEvents()
                slavesAccess
                        .withLock {slaves.map {it.value}}
                        .forEach {it.apply(events)}
            }
            while (events != null)
        }

        init
        {
            masterWorker.name = ::masterWorker.name
            masterWorker.start()
        }

        override fun add(slave:Slave<Event,Message>):Unit = slavesAccess.withLock()
        {
            // throw exception if slave already added
            require(slave !in slaves.keys)

            // register the new slave
            val slaveEntry = SlaveEntry(slave,threadFactory,master)
            slaves[slave] = slaveEntry
            slaveEntry.worker.start()

            // send the new slave initialization ticks while we have the lock
            slaveEntry.apply(master.generateSnapshot())
        }

        override fun rm(slave:Slave<Event,Message>):Unit = slavesAccess.withLock()
        {
            // remove the slave
            val existingEntry = slaves.remove(slave)

            // tell slave that it is over, and wait for thread to stop
            if (existingEntry != null)
            {
                existingEntry.apply(null)
                existingEntry.close()
            }
        }

        override fun close()
        {
            slavesAccess.withLock()
            {
                slaves.keys.toList().forEach {rm(it)}
                state = this@Synchronizer.Closed()
                master.close()
            }
            masterWorker.join()
        }
    }

    private inner class Closed<Event,Message>:State<Event,Message>
    {
        override fun add(slave:Slave<Event,Message>) = throw IllegalStateException("closed")
        override fun rm(slave:Slave<Event,Message>) = throw IllegalStateException("closed")
        override fun close() = throw IllegalStateException("closed")
    }
}

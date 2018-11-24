package com.github.ericytsang.lib.sync

import java.io.Closeable
import java.util.concurrent.ThreadFactory
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class Synchronizer<Event,Request>(
        master:Master<Event,Request>,
        threadFactory:ThreadFactory)
    :
        Closeable
{
    private var state:State<Event,Request> = Opened(master,threadFactory)
    private val stateAccess = ReentrantLock()
    fun add(slave:Slave<Event,Request>) = stateAccess.withLock {state.add(slave)}
    fun rm(slave:Slave<Event,Request>) = stateAccess.withLock {state.rm(slave)}
    override fun close() = stateAccess.withLock {state.close()}

    private interface State<Event,Message>
    {
        fun add(slave:Slave<Event,Message>)
        fun rm(slave:Slave<Event,Message>)
        fun close()
    }

    private inner class Opened(
            _master:Master<Event,Request>,
            val threadFactory:ThreadFactory)
        :
            State<Event,Request>
    {
        private val master:Master<Event,Request> = SynchronizedMaster(_master)

        private val slavesAccess = ReentrantLock()
        private val slaves = mutableMapOf<Slave<Event,Request>,Slave<Event,Request>>()
            get()
            {
                check(slavesAccess.isHeldByCurrentThread)
                return field
            }

        private val masterWorker = threadFactory.newThread()
        {
            while (true)
            {
                // get pending events to broadcast to slaves
                val events = master.getPendingEvents() ?: break

                // broadcast events to slaves
                slavesAccess
                        .withLock {slaves.map {it.value}}
                        .forEach {it.apply(events)}
            }
            slavesAccess.withLock()
            {
                slaves.values.toList().forEach {rm(it)}
            }
        }

        init
        {
            masterWorker.name = ::masterWorker.name
            masterWorker.start()
        }

        override fun add(slave:Slave<Event,Request>):Unit = slavesAccess.withLock()
        {
            val initializationEvents = master.generateSnapshot()?:return@withLock close()

            // throw exception if slave already added
            require(slave !in slaves)

            // register the new slave
            slaves[slave] = slave

            // send the new slave initialization ticks while we have the lock
            slave.apply(initializationEvents)
        }

        override fun rm(slave:Slave<Event,Request>):Unit = slavesAccess.withLock()
        {
            // get slave from set
            val existingEntry = slaves[slave]

            // remove, and tell slave that it is over, and wait for thread to stop
            if (existingEntry != null)
            {
                slaves.remove(slave)
                existingEntry.close()
            }
        }

        override fun close()
        {
            state = this@Synchronizer.Closed()
            master.close()
            masterWorker.join()
        }
    }

    private inner class Closed:State<Event,Request>
    {
        override fun add(slave:Slave<Event,Request>) = throw IllegalStateException("closed")
        override fun rm(slave:Slave<Event,Request>) = throw IllegalStateException("closed")
        override fun close() = Throwable("already closed. redundant call to close").printStackTrace(System.out)
    }

    private inner class SynchronizedMaster(
            private val delegate:Master<Event,Request>)
        :Master<Event,Request>
    {
        private val lock = ReentrantLock()

        override fun getPendingEvents():List<Event>? = lock.withLock()
        {
            delegate.getPendingEvents()
        }

        override fun generateSnapshot():List<Event>? = lock.withLock()
        {
            delegate.generateSnapshot()
        }

        override fun process(requests:List<Request>) = lock.withLock()
        {
            delegate.process(requests)
        }

        override fun close() = lock.withLock()
        {
            delegate.close()
        }
    }
}
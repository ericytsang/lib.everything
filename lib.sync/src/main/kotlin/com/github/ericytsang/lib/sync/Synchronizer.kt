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
        set(value)
        {
            check(stateAccess.isHeldByCurrentThread)
            field = value
        }
        get()
        {
            check(stateAccess.isHeldByCurrentThread)
            return field
        }
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
        private val slaves = mutableMapOf<Slave<Event,Request>,SlaveWithWorker>()
            get()
            {
                check(slavesAccess.isHeldByCurrentThread)
                return field
            }

        private val masterWorker:Thread = threadFactory.newThread()
        {
            while (true)
            {
                // get pending events to broadcast to slaves
                val events = master.getPendingEvents() ?: break

                // broadcast events to slaves
                slavesAccess
                        .withLock {slaves.map {it.value}}
                        .forEach {it.slave.apply(events)}
            }
            stateAccess.withLock {state = this@Synchronizer.Closed()}
            slavesAccess
                    .withLock {slaves.map {it.value}}
                    .forEach {it.close()}
        }

        init
        {
            masterWorker.name = ::masterWorker.name
            masterWorker.start()
        }

        override fun add(slave:Slave<Event,Request>):Unit = slavesAccess.withLock()
        {
            val initializationEvents = master.generateSnapshot()
                    ?:return

            // throw exception if slave already added
            require(slave !in slaves)

            // register the new slave
            val newSlave = SlaveWithWorker(slave)
            slaves[slave] = newSlave

            // send the new slave initialization ticks while we have the lock
            newSlave.slave.apply(initializationEvents)

            // start processing requests from this slave
            newSlave.worker.start()
        }

        override fun rm(slave:Slave<Event,Request>)
        {
            slavesAccess.withLock {slaves[slave]}?.close()
        }

        override fun close()
        {
            master.close()
            masterWorker.join()
        }

        private inner class SlaveWithWorker(
                val slave:Slave<Event, Request>)
            :Closeable
        {
            val worker:Thread = threadFactory.newThread()
            {
                while (true)
                {
                    // get pending events to broadcast to slaves
                    val requests = slave.getPendingRequests()?:break

                    // send requests to master
                    master.process(requests)
                }
                slavesAccess.withLock {slaves.remove(slave)}
            }

            override fun close()
            {
                slave.close()
                worker.join()
            }
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
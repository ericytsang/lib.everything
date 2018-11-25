package com.github.ericytsang.lib.sync

import com.github.ericytsang.lib.liablethread.LiabilityEnforcingThreadFactory
import java.io.Closeable
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class Synchronizer<Event,Request>(
        master:Master<Request>,
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

    /**
     * queue up events to broadcast to all connected [Slave]s in the near future.
     *
     */
    fun broadcastEvents(events:List<Event>) = stateAccess.withLock {state.broadcastEvents(events)}

    /**
     * queue up events to broadcast to all new [Slave]s in the near future.
     * these events are always the first events that a [Slave] receives upon
     * connecting.
     */
    fun broadcastSnapshot(events:List<Event>) = stateAccess.withLock {state.broadcastSnapshot(events)}

    private interface State<Event,Message>
    {
        fun add(slave:Slave<Event,Message>)
        fun rm(slave:Slave<Event,Message>)
        fun close()
        fun broadcastEvents(events:List<Event>)
        fun broadcastSnapshot(events:List<Event>)
    }

    private inner class Opened(
            _master:Master<Request>,
            _threadFactory:ThreadFactory)
        :
            State<Event,Request>
    {
        private val threadFactory:ThreadFactory = LiabilityEnforcingThreadFactory(_threadFactory)
        private val ticks = LinkedBlockingQueue<Tick<Event,Request>>()

        private val worker:Thread = threadFactory.newThread()
        {
            val context = Context<Event,Request>(_master,mutableMapOf(),mutableSetOf())
            do
            {
                val tick = ticks.take()

                val shouldBreak = when (tick)
                {
                    is Tick.Broacast ->
                    {
                        context.slaves.forEach()
                        {
                            it.value.slave.apply(tick.events)
                        }
                        false
                    }
                    is Tick.Snapshot ->
                    {
                        context.virginSlaves.toList().forEach()
                        {
                            virgin ->

                            // send the virgin the snapshot events
                            virgin.apply(tick.events)

                            // move slave to non-virgin area
                            val newSlave = SlaveWithWorker(virgin,threadFactory,ticks)
                            context.slaves[virgin] = newSlave
                            context.virginSlaves.remove(virgin)
                        }
                        false
                    }
                    is Tick.Closure ->
                    {
                        tick.block(context)
                        false
                    }
                    is Tick.Poison ->
                    {
                        check(ticks.isEmpty())
                        context.virginSlaves.clear()
                        context.slaves.forEach {it.value.close()}
                        context.slaves.clear()
                        if (!ticks.isEmpty())
                        {
                            ticks += tick
                        }
                        ticks.isEmpty()
                    }
                }
            }
            while (!shouldBreak)
        }

        init
        {
            worker.name = "${::worker.name}@${Synchronizer::class.simpleName}"
            worker.start()
        }

        override fun add(slave:Slave<Event,Request>)
        {
            ticks += Tick.Closure()
            {
                context ->

                // throw exception if slave already added
                require(slave !in context.slaves.keys)
                require(slave !in context.virginSlaves)

                // queue up the slave for registration
                context.virginSlaves += slave
                context.master.requestSnapshot()
            }
        }

        override fun rm(slave:Slave<Event,Request>)
        {
            ticks += Tick.Closure()
            {
                context ->
                context.virginSlaves.remove(slave)
                context.slaves[slave]?.close()
                context.slaves.remove(slave)
                Unit
            }
        }

        override fun close()
        {
            ticks += Tick.Closure()
            {
                context ->
                context.master.close()
                ticks += Tick.Poison()
            }
            worker.join()
            state = Closed()
        }

        override fun broadcastEvents(events:List<Event>)
        {
            ticks += Tick.Broacast(events)
        }

        override fun broadcastSnapshot(events:List<Event>)
        {
            ticks += Tick.Snapshot(events)
        }
    }

    private inner class Closed:State<Event,Request>
    {
        override fun add(slave:Slave<Event,Request>) = throw IllegalStateException("closed")
        override fun rm(slave:Slave<Event,Request>) = throw IllegalStateException("closed")
        override fun close() = Throwable("already closed. redundant call to close").printStackTrace(System.out)
        override fun broadcastEvents(events:List<Event>) = throw IllegalStateException("closed")
        override fun broadcastSnapshot(events:List<Event>) = throw IllegalStateException("closed")
    }

    private class SlaveWithWorker<Event,Request>(
            val slave:Slave<Event,Request>,
            private val threadFactory:ThreadFactory,
            private val ticks:LinkedBlockingQueue<Tick<Event,Request>>)
        :Closeable
    {
        private val worker:Thread = threadFactory.newThread()
        {
            while (true)
            {
                // get pending events to broadcast to slaves
                val requests = slave.getPendingRequests()?:break

                // send requests to master
                ticks += Tick.Closure()
                {
                    context ->
                    context.master.process(requests)
                }
            }
            ticks += Tick.Closure()
            {
                context ->
                if (!closeWasAlreadyCalled)
                {
                    closeWasAlreadyCalled = true
                    slave.close()
                }
                context.slaves.remove(slave)
                Unit
            }
        }

        init
        {
            worker.start()
        }

        private var closeWasAlreadyCalled = false
        override fun close()
        {
            closeWasAlreadyCalled = true
            slave.close()
            worker.join()
        }
    }

    private class Context<Event,Request>(
            val master:Master<Request>,
            val slaves:MutableMap<Slave<Event,Request>,SlaveWithWorker<Event,Request>>,
            val virginSlaves:MutableSet<Slave<Event,Request>>)

    private sealed class Tick<Event,Request>
    {
        class Broacast<Event,Request>(
                val events:List<Event>)
            :Tick<Event,Request>()

        class Snapshot<Event,Request>(
                val events:List<Event>)
            :Tick<Event,Request>()

        class Poison<Event,Request>
            :Tick<Event,Request>()

        class Closure<Event,Request>(
                val block:(Context<Event,Request>)->Unit)
            :Tick<Event,Request>()
    }
}
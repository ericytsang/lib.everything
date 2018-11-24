package com.github.ericytsang.lib.sync

import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ThreadFactory
import kotlin.concurrent.thread
import kotlin.test.assertEquals

class BroadcastingEventsAndSnapshotsTest
{
    private val events = (0..3).map {Event(it)}

    private val master = spy(MockMaster())
    private val slaves = (0..1).map {spy(MockSlave())}

    private val unlatchToLetThreadsRun = CountDownLatch(1)
    private val fixture = Synchronizer(master,ThreadFactory()
    {
        thread(start = false)
        {
            unlatchToLetThreadsRun.await()
            it.run()
        }
    })

    @Test
    fun can_use_multiple_thenAnswer_calls()
    {
        val mock = mock(CharSequence::class.java)
        `when`(mock.length)
                .thenAnswer {5}
                .thenAnswer {6}
        assertEquals(mock.length,5)
        assertEquals(mock.length,6)
        assertEquals(mock.length,6)
    }

    @Test
    fun can_shut_down_master_worker_thread()
    {
        `when`(master.getPendingEvents()).thenReturn(null)
        unlatchToLetThreadsRun.countDown()
        fixture.close()
    }

    @Test
    fun slaves_receive_snapshots_from_generateSnapshot_upon_connecting()
    {
        `when`(master.getPendingEvents()).thenReturn(null)
        `when`(master.generateSnapshot()).thenReturn(events)

        // connect slaves to the fixture
        slaves.forEach {fixture.add(it)}

        // let the threads start running after saves are connected
        unlatchToLetThreadsRun.countDown()

        // wait for the threads to stop running
        fixture.close()

        // should generate a snapshot for every slave that connected
        verify(master,times(slaves.size)).generateSnapshot()

        // getPendingEvents should be called once for returning null
        verify(master).getPendingEvents()

        // slaves should have been given the events from the master
        for (slave in slaves)
        {
            verify(slave).apply(events)
        }
    }

    @Test
    fun events_received_after_slaves_are_connected_are_sent_to_slaves()
    {
        `when`(master.getPendingEvents())
                .thenReturn(events)
                .thenReturn(null)

        // connect slaves to the fixture
        slaves.forEach {fixture.add(it)}

        // let the threads start running after saves are connected
        unlatchToLetThreadsRun.countDown()

        // wait for the threads to stop running
        fixture.close()

        // should generate a snapshot for every slave that connected
        verify(master,times(slaves.size)).generateSnapshot()

        // getPendingEvents should be called twice.. once for the events, and
        // the second time, returning null
        verify(master,times(2)).getPendingEvents()

        // slaves should have been given the events from the master
        for (slave in slaves)
        {
            verify(slave).apply(events)
        }
    }

    open class MockMaster:Master<Event,Request>
    {
        override fun getPendingEvents():List<Event>? = emptyList()
        override fun generateSnapshot():List<Event>? = emptyList()
        override fun process(requests:List<Request>) = Unit
        override fun close() = Unit
    }
    open class MockSlave:Slave<Event,Request>
    {
        override fun apply(events:List<Event>) = Unit
        override fun getPendingRequests():List<Request>? = emptyList()
        override fun close() = Unit
    }
    data class Event(val data:Int)
    data class Request(val data:Int)
}
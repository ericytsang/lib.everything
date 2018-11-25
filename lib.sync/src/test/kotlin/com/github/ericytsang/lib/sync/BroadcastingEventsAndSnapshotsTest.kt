package com.github.ericytsang.lib.sync

import com.github.ericytsang.lib.testutils.TestUtils
import org.junit.After
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.never
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

class BroadcastingEventsAndSnapshotsTest
{
    private val events1 = (0..3).map {Event(it)}
    private val events2 = (4..6).map {Event(it)}

    private val master = spy(MockMaster())
    private val slaves = (0..1).map {spy(MockSlave())}
    private val fixture = Synchronizer<Event,Request>(master,Executors.defaultThreadFactory())

    init
    {
        for (slave in slaves)
        {
            val latch = CountDownLatch(1)
            `when`(slave.close()).thenAnswer()
            {
                latch.countDown()
            }
            `when`(slave.getPendingRequests()).thenAnswer()
            {
                latch.await()
                null
            }
        }

        // connect slaves to the fixture
        slaves.forEach {fixture.add(it)}
    }

    @After
    fun teardown()
    {
        TestUtils.assertAllWorkerThreadsDead()
    }

    @Test
    fun can_shut_down_master_worker_thread()
    {
        fixture.close()
        verify(master).close()
    }

    @Test
    fun slaves_receive_snapshots_from_broadcastSnapshot_upon_connecting()
    {
        // broadcast some events
        fixture.broadcastEvents(events1)
        fixture.broadcastSnapshot(events2)

        // wait for the threads to stop running
        fixture.close()

        // should request a snapshot once for each connected slave
        verify(master,times(slaves.size)).requestSnapshot()

        for (slave in slaves)
        {
            // should not receive events before receiving the snapshots...
            // events broadcasted before receiving the snapshot are not
            // relevant, and should not be received by the slave at all.
            verify(slave,never()).apply(events1)

            // slaves should have been given the snapshot events from master
            verify(slave).apply(events2)

            // all slaves should be closed
            verify(slave).close()
        }
    }

    @Test
    fun events_received_after_slaves_are_connected_are_sent_to_slaves()
    {
        // broadcast some events
        fixture.broadcastSnapshot(events1)
        fixture.broadcastEvents(events2)

        // wait for the threads to stop running
        fixture.close()

        // should request a snapshot once for each connected slave
        verify(master,times(slaves.size)).requestSnapshot()

        for (slave in slaves)
        {
            // slaves should have been given the snapshot events from master
            verify(slave).apply(events1)
            verify(slave).apply(events2)

            // all slaves should be closed
            verify(slave).close()
        }
    }
}

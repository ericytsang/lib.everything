package com.github.ericytsang.lib.sync

import com.github.ericytsang.lib.testutils.TestUtils
import org.junit.After
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ThreadFactory
import kotlin.concurrent.thread

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

    @After
    fun teardown()
    {
        TestUtils.assertAllWorkerThreadsDead()
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
        for (slave in slaves)
        {
            `when`(slave.close()).thenAnswer()
            {
                `when`(slave.getPendingRequests()).thenReturn(null)
            }
        }

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
        for (slave in slaves)
        {
            `when`(slave.close()).thenAnswer()
            {
                `when`(slave.getPendingRequests()).thenReturn(null)
            }
        }

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
}

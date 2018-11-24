package com.github.ericytsang.lib.sync

import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ThreadFactory
import kotlin.concurrent.thread

class RequestProcessingTest
{
    private val events = (0..3).map {Event(it)}

    private val master = spy(MockMaster())
    private val slaves = (0..1).map {Mockito.spy(MockSlave())}

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
    fun master_receives_requests_from_slaves()
    {
        val unblockWhenMasterCanShutDown = CountDownLatch(2)
        `when`(master.generateSnapshot()).thenReturn(events)
        `when`(master.close()).thenAnswer()
        {
            `when`(master.getPendingEvents()).thenReturn(null)
        }
        `when`(slaves[0].getPendingRequests())
                .thenAnswer {listOf(Request(4))}
                .thenAnswer()
                {
                    unblockWhenMasterCanShutDown.countDown()
                    null
                }
        `when`(slaves[1].getPendingRequests())
                .thenAnswer {listOf(Request(5))}
                .thenAnswer {listOf(Request(6),Request(7))}
                .thenAnswer()
                {
                    unblockWhenMasterCanShutDown.countDown()
                    null
                }
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
        unblockWhenMasterCanShutDown.await()
        fixture.close()

        // should generate a snapshot for every slave that connected
        verify(master).process(listOf(Request(4)))
        verify(master).process(listOf(Request(5)))
        verify(master).process(listOf(Request(6),Request(7)))
    }
}

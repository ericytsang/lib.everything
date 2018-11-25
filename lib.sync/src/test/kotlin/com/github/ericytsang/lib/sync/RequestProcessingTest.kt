package com.github.ericytsang.lib.sync

import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

class RequestProcessingTest
{
    private val master = spy(MockMaster())
    private val slaves = (0..1).map {spy(MockSlave())}
    private val fixture = Synchronizer<Event,Request>(master,Executors.defaultThreadFactory())

    @Test
    fun master_receives_requests_from_slaves()
    {
        val unblockWhenMasterCanShutDown = CountDownLatch(2)
        `when`(master.requestSnapshot())
                .thenAnswer {fixture.broadcastSnapshot(emptyList())}
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

        // connect slaves to the fixture
        slaves.forEach {fixture.add(it)}

        // wait for slaves to finish sending requests
        unblockWhenMasterCanShutDown.await()

        // close fixture and wait for threads to stop running
        fixture.close()

        // should generate a snapshot for every slave that connected
        verify(master).process(listOf(Request(4)))
        verify(master).process(listOf(Request(5)))
        verify(master).process(listOf(Request(6),Request(7)))

        for (slave in slaves)
        {
            // all slaves should be closed
            verify(slave).close()
        }
    }
}

package com.github.ericytsang.lib.sync

import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify

class RequestProcessingTest
{
    private val master = spy(MockMaster())
    private val slaves = (0..1).map {spy(MockSlave())}
    private val listener = mock(Synchronizer.Listener::class.java)
    private val fixture = Synchronizer(master,listener)

    @Test
    fun master_receives_requests_from_self()
    {
        // broadcast some events before clients are connected
        val requests1 = (1..9).map {Request(it)}
        `when`(master.getPendingRequests()).thenReturn(requests1)
        fixture.cycle()

        // should apply the requests sent by master
        verify(master).process(requests1)
    }

    @Test
    fun master_receives_requests_from_slaves()
    {
        // connect slaves to the fixture
        slaves.forEach {fixture.add(it)}

        // broadcast some events before clients are connected
        val requests1 = (1..9).map {Request(it)}
        val requests2 = (2..9).map {Request(it)}
        `when`(slaves[0].getPendingRequests()).thenReturn(requests1)
        `when`(slaves[1].getPendingRequests()).thenReturn(requests2)
        fixture.cycle()

        // should apply the requests sent by master
        verify(master).process(requests1)
        verify(master).process(requests2)
    }
}

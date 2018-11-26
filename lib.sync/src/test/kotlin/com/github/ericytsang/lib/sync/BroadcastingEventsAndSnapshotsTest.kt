package com.github.ericytsang.lib.sync

import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

class BroadcastingEventsAndSnapshotsTest
{
    private val events1 = (1..9).map {Event(it)}
    private val events2 = (2..9).map {Event(it)}
    private val events3 = (3..9).map {Event(it)}
    private val events4 = (4..9).map {Event(it)}

    private val master = spy(MockMaster())
    private val slaves = (0..1).map {spy(MockSlave())}
    private val listener = mock(Synchronizer.Listener::class.java)
    private val fixture = Synchronizer(master,listener)

    @Test
    fun slaves_receive_snapshots_from_broadcastSnapshot_upon_connecting()
    {
        // broadcast some events before clients are connected
        `when`(master.generateSnapshot()).thenReturn(events1)
        `when`(master.getPendingEvents()).thenReturn(events2)
        fixture.cycle()

        // should not generate a snapshot at this point as there have been no
        // new slaves
        verify(master,never()).generateSnapshot()

        // should call getPendingEvents once per cycle
        verify(master).getPendingEvents()

        // connect slaves to the fixture
        slaves.forEach {fixture.add(it)}

        // broadcast some events
        `when`(master.generateSnapshot()).thenReturn(events3)
        `when`(master.getPendingEvents()).thenReturn(events4)
        fixture.cycle()

        // should generate a snapshot once in total to generate snapshot for the
        // new slaves
        verify(master).generateSnapshot()

        // should call getPendingEvents once per cycle
        verify(master,times(2)).getPendingEvents()

        for (slave in slaves)
        {
            // should not receive events before receiving the snapshots...
            // events broadcasted before receiving the snapshot are not
            // relevant, and should not be received by the slave at all.
            verify(slave,never()).apply(events1)
            verify(slave,never()).apply(events2)

            // slaves should have been given the snapshot events from master
            verify(slave).apply(events3)
            verify(slave).apply(events4)
        }
    }

    @Test
    fun events_received_after_slaves_are_connected_are_sent_to_slaves_for_1_cycle()
    {
        // connect slaves to the fixture
        slaves.forEach {fixture.add(it)}

        // broadcast some events
        `when`(master.generateSnapshot()).thenReturn(events1)
        `when`(master.getPendingEvents()).thenReturn(events2)
        fixture.cycle()

        // should generate a snapshot once in total to generate snapshot for the
        // new slaves
        verify(master).generateSnapshot()

        // should have received new events from snapshot and cycle
        for (slave in slaves)
        {
            verify(slave).apply(events1)
            verify(slave).apply(events2)
            verify(slave,never()).apply(events3)
            verify(slave,never()).apply(events4)
        }
    }

    @Test
    fun events_received_after_slaves_are_connected_are_sent_to_slaves_for_2_cycles()
    {
        events_received_after_slaves_are_connected_are_sent_to_slaves_for_1_cycle()

        // broadcast some more events
        `when`(master.getPendingEvents()).thenReturn(events3)
        fixture.cycle()

        // should call getPendingEvents once per cycle
        verify(master,times(2)).getPendingEvents()

        // should have received new events from cycle
        for (slave in slaves)
        {
            verify(slave).apply(events1)
            verify(slave).apply(events2)
            verify(slave).apply(events3)
            verify(slave,never()).apply(events4)
        }
    }

    @Test
    fun events_received_after_slaves_are_connected_are_sent_to_slaves_for_3_cycles()
    {
        events_received_after_slaves_are_connected_are_sent_to_slaves_for_2_cycles()

        // broadcast even more events
        `when`(master.getPendingEvents()).thenReturn(events4)
        fixture.cycle()

        // should not have called this method again since the first time
        verify(master).generateSnapshot()

        // should call getPendingEvents once per cycle
        verify(master,times(3)).getPendingEvents()

        // should have received new events from cycle
        for (slave in slaves)
        {
            verify(slave).apply(events1)
            verify(slave).apply(events2)
            verify(slave).apply(events3)
            verify(slave).apply(events4)
        }
    }
}

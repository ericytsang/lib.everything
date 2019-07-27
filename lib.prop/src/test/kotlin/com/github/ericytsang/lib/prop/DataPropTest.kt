package com.github.ericytsang.lib.prop

import org.junit.Test
import java.util.concurrent.LinkedBlockingQueue
import kotlin.test.assertEquals
import kotlin.test.fail

class DataPropTest
{

    @Test
    fun get_gets_value_from_set_using_extension_methods()
    {
        val dataProp = DataProp(1)
        dataProp.value = 2
        assertEquals(2,dataProp.value)
    }

    @Test
    fun get_gets_value_from_set()
    {
        val dataProp = DataProp(1)
        dataProp.set(Unit,2)
        assertEquals(2,dataProp.get(Unit))
    }

    @Test
    fun get_gets_value_from_constructor()
    {
        val dataProp = DataProp(1)
        assertEquals(1,dataProp.value)
    }

    @Test
    fun set_calls_listeners()
    {
        val dataProp = DataProp(1)
        val onChangedInvocations = LinkedBlockingQueue<Pair<ReadOnlyProp<Unit,Int>,Int>>()
        fun getCallsSoFar() = generateSequence {onChangedInvocations.poll()}.toList()

        dataProp.listen {onChangedInvocations += it to it.value}
        assert(getCallsSoFar().isEmpty())

        dataProp.value = 3
        assertEquals(
                listOf(
                        dataProp to 3),
                getCallsSoFar())

        dataProp.value = 4
        assertEquals(
                listOf(
                        dataProp to 4),
                getCallsSoFar())

        dataProp.value = 5
        assertEquals(
                listOf(
                        dataProp to 5),
                getCallsSoFar())
    }

    @Test
    fun listener_setting_prop_throws()
    {
        val dataProp = DataProp(1)
        dataProp.listen {dataProp.value = 3}
        try
        {
            dataProp.value = 2
            fail("exception expected")
        }
        catch (ex:Prop.RecursiveSettingIsNotAllowedException)
        {
            ex.printStackTrace(System.out)
        }
    }

    @Test
    fun listeners_do_not_get_notified_after_unsubscribing()
    {
        val dataProp = DataProp(1)
        val onChangedInvocations = LinkedBlockingQueue<Pair<ReadOnlyProp<Unit,Int>,Int>>()
        fun getCallsSoFar() = generateSequence {onChangedInvocations.poll()}.toList()

        val listener = dataProp.listen {onChangedInvocations += it to it.value}
        assert(getCallsSoFar().isEmpty())

        dataProp.value = 3
        assertEquals(
                listOf(
                        dataProp to 3),
                getCallsSoFar())

        listener.close()
        dataProp.value = 4
        assert(getCallsSoFar().isEmpty())

        dataProp.value = 5
        assert(getCallsSoFar().isEmpty())
    }

    @Test
    fun get_does_not_notify_listeners()
    {
        val dataProp = DataProp(1)
        val onChangedInvocations = LinkedBlockingQueue<Pair<ReadOnlyProp<Unit,Int>,Int>>()
        fun getCallsSoFar() = generateSequence {onChangedInvocations.poll()}.toList()

        dataProp.listen {onChangedInvocations += it to it.value}
        assert(getCallsSoFar().isEmpty())

        dataProp.value
        assert(getCallsSoFar().isEmpty())

        dataProp.value
        assert(getCallsSoFar().isEmpty())

        dataProp.value
        assert(getCallsSoFar().isEmpty())
    }

    @Test
    fun smoke_test()
    {
        val dataProp = DataProp(1)
        val onChangedInvocations1 = LinkedBlockingQueue<Pair<ReadOnlyProp<Unit,Int>,Int>>()
        val onChangedInvocations2 = LinkedBlockingQueue<Pair<ReadOnlyProp<Unit,Int>,Int>>()
        fun getCallsSoFar1() = generateSequence {onChangedInvocations1.poll()}.toList()
        fun getCallsSoFar2() = generateSequence {onChangedInvocations2.poll()}.toList()

        val listener = dataProp.listen {onChangedInvocations1 += it to it.value}
        dataProp.listen {onChangedInvocations2 += it to it.value}
        assert(getCallsSoFar1().isEmpty())
        assert(getCallsSoFar2().isEmpty())

        dataProp.value = 3
        assertEquals(
                listOf(
                        dataProp to 3),
                getCallsSoFar1())
        assertEquals(
                listOf(
                        dataProp to 3),
                getCallsSoFar2())

        listener.close()
        dataProp.value = 4
        assert(getCallsSoFar1().isEmpty())
        assertEquals(
                listOf(
                        dataProp to 4),
                getCallsSoFar2())

        dataProp.value = 5
        assert(getCallsSoFar1().isEmpty())
        assertEquals(
                listOf(
                        dataProp to 5),
                getCallsSoFar2())
    }
}
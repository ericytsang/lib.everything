package com.github.ericytsang.lib.prop

import org.junit.Test
import java.util.concurrent.LinkedBlockingQueue
import kotlin.test.assertEquals

class ExtensionsKtTest
{
    private val events = LinkedBlockingQueue<String>()
    private fun eventsSoFar() = generateSequence {events.poll()}.toList()

    private val prop = object:Prop<String,String,Int>()
    {
        private val dataProp = DataProp(1)
        override fun doGet(context:String):Int = dataProp.value
        override fun doSet(readContext:String,writeContext:String,value:Int)
        {
            dataProp.value = value
        }
    }
    private val withContextProp = prop.withReadWriteContext {""}
    private val mappedProp = prop.map {it+1}

    @Test
    fun updating_prop_notifies_listeners_of_withContextProp()
    {
        withContextProp.listen()
        {
            events += withContextProp.nullableValue.toString()
            events += "${it.oldValue} -> ${it.newValue}"
        }
        assert(eventsSoFar().isEmpty())
        prop.set("","",2)
        assertEquals(
                listOf(
                        "null",
                        "1 -> 2",
                        "2",
                        "1 -> 2"),
                eventsSoFar())
    }

    @Test
    fun updating_prop_notifies_listeners_of_mappedProp()
    {
        mappedProp.listen()
        {
            events += mappedProp.getNullable("").toString()
            events += "${it.oldValue} -> ${it.newValue}"
        }
        assert(eventsSoFar().isEmpty())
        prop.set("","",2)
        assertEquals(
                listOf(
                        "null",
                        "2 -> 3",
                        "3",
                        "2 -> 3"),
                eventsSoFar())
    }
}

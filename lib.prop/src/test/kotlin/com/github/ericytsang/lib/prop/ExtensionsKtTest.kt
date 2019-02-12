package com.github.ericytsang.lib.prop

import org.junit.Test
import java.util.concurrent.LinkedBlockingQueue
import kotlin.test.assertEquals

class ExtensionsKtTest
{
    private val events = LinkedBlockingQueue<String>()
    private fun eventsSoFar() = generateSequence {events.poll()}.toList()

    private val prop = object:Prop<String,Int>()
    {
        private val dataProp = DataProp(1)
        override fun doGet(context:String):Int = dataProp.value
        override fun doSet(context:String,value:Int)
        {
            dataProp.value = value
        }
    }
    private val withContextProp = prop.withContext {""}
    private val mappedProp = prop.map {it+1}

    @Test
    fun updating_prop_notifies_listeners_of_withContextProp()
    {
        withContextProp.listen()
        {
            events += withContextProp.getChange().run {"$oldValue => $newValue"}
            events += "${it.oldValue} -> ${it.newValue}"
        }
        assert(eventsSoFar().isEmpty())
        prop.set("",2)
        assertEquals(
                listOf(
                        "1 => 2",
                        "1 -> 2"),
                eventsSoFar())
    }

    @Test
    fun updating_prop_notifies_listeners_of_mappedProp()
    {
        mappedProp.listen()
        {
            events += mappedProp.getChange().run {"$oldValue => $newValue"}
            events += "${it.oldValue} -> ${it.newValue}"
        }
        assert(eventsSoFar().isEmpty())
        prop.set("",2)
        assertEquals(
                listOf(
                        "2 => 3",
                        "2 -> 3"),
                eventsSoFar())
    }
}

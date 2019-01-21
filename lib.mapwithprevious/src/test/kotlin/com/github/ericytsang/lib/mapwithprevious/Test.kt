package com.github.ericytsang.lib.mapwithprevious

import org.junit.Test
import kotlin.test.assertEquals

class Test
{
    @Test
    fun map_over_sequence()
    {
        val actual = sequenceOf(0,1,2,3,4,5)
                .mapWithPrevious {p:Float?,n -> n.toFloat()+(p?:0f)}
                .toList()
        assertEquals(listOf(0f,1f,3f,6f,10f,15f),actual)
    }

    @Test
    fun map_over_iterable()
    {
        val actual = listOf(0,1,2,3,4,5)
                .mapWithPrevious {p:Float?,n -> n.toFloat()+(p?:0f)}
                .toList()
        assertEquals(listOf(0f,1f,3f,6f,10f,15f),actual)
    }
}

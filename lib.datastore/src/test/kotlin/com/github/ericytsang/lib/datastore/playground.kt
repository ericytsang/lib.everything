package com.github.ericytsang.lib.datastore

import kotlin.test.Test
import kotlin.test.assertEquals

class RandomTests
{
    val list = listOf(0,1,2,3,4,5,6,7,8,9,10)

    @Test
    fun list_iterator_next_index()
    {
        assertEquals(9,list.listIterator(9).nextIndex())
    }

    @Test
    fun linked_hash_set_moves_item_from_middle_to_beginning_again_if_accessed()
    {
        val set = LinkedHashSet<Int>()
        set.put(1)
        set.put(2)
        set.put(3)
        set.put(4)
        set.put(5)
        println(set.toList())
        set.put(1)
        println(set.toList())
    }

    fun <E> MutableSet<E>.put(element:E)
    {
        remove(element)
        add(element)
    }
}

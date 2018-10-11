package com.github.ericytsang.lib.simplifiedmap

import org.junit.Test

class SimplifiedMapWrapperTest
{
    val map = mutableMapOf("a" to 3,"b" to 4)
        .let {SimplifiedMapWrapper(it)}

    @Test
    fun get_test()
    {
        assert(map["a"] == 3)
        assert(map["b"] == 4)
        assert(map["c"] == null)
    }

    @Test
    fun remove_test()
    {
        assert(map["a"] == 3)
        assert(map["b"] == 4)
        assert(map["c"] == null)
        map["b"] = null
        assert(map["a"] == 3)
        assert(map["b"] == null)
        assert(map["c"] == null)
    }

    @Test
    fun set_test()
    {
        assert(map["a"] == 3)
        assert(map["b"] == 4)
        assert(map["c"] == null)
        map["b"] = 7
        assert(map["a"] == 3)
        assert(map["b"] == 7)
        assert(map["c"] == null)
    }

    @Test
    fun clear_test()
    {
        assert(map["a"] == 3)
        assert(map["b"] == 4)
        map.clear()
        assert(map["a"] == null)
        assert(map["b"] == null)
    }
}

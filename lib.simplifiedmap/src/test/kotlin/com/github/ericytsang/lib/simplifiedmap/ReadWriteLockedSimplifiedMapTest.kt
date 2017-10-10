package com.github.ericytsang.lib.simplifiedmap

import com.github.ericytsang.lib.testutils.TestUtils.exceptionExpected
import org.junit.Test
import kotlin.concurrent.write

class ReadWriteLockedSimplifiedMapTest
{
    val map = mutableMapOf("a" to 3,"b" to 4)
        .let {SimplifiedMapWrapper(it)}
        .let {ReadWriteLockedSimplifiedMapWrapper(it)}

    @Test
    fun get_test()
    {
        assert(map["a"] == 3)
        assert(map["b"] == 4)
        assert(map["c"] == null)
    }

    @Test
    fun remove_test_with_lock()
    {
        assert(map["a"] == 3)
        assert(map["b"] == 4)
        assert(map["c"] == null)
        map.readWriteLock.write {map["b"] = null}
        assert(map["a"] == 3)
        assert(map["b"] == null)
        assert(map["c"] == null)
    }

    @Test
    fun set_test_with_lock()
    {
        assert(map["a"] == 3)
        assert(map["b"] == 4)
        assert(map["c"] == null)
        map.readWriteLock.write {map["b"] = 7}
        assert(map["a"] == 3)
        assert(map["b"] == 7)
        assert(map["c"] == null)
    }

    @Test
    fun remove_test_no_lock()
    {
        // check state
        assert(map["a"] == 3)
        assert(map["b"] == 4)
        assert(map["c"] == null)

        // try to manipulate map
        exceptionExpected {
            map["b"] = null
        }

        // state should not have changed
        assert(map["a"] == 3)
        assert(map["b"] == 4)
        assert(map["c"] == null)
    }

    @Test
    fun set_test_no_lock()
    {
        // check state
        assert(map["a"] == 3)
        assert(map["b"] == 4)
        assert(map["c"] == null)

        // try to manipulate map
        exceptionExpected {
            map["b"] = 7
        }

        // state should not have changed
        assert(map["a"] == 3)
        assert(map["b"] == 4)
        assert(map["c"] == null)
    }
}

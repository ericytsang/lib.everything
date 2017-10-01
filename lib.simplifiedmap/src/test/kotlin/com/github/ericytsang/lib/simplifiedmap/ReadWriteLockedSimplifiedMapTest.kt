package com.github.ericytsang.lib.simplifiedmap

import org.junit.Test
import kotlin.concurrent.write

class ReadWriteLockedSimplifiedMapTest
{
    val map = mutableMapOf("a" to 3,"b" to 4)
        .let {SimplifiedMapWrapper(it)}
        .let {ReadWriteLockedSimplifiedMapWrapper(it)}

    @Test
    fun getTest()
    {
        assert(map["a"] == 3)
        assert(map["b"] == 4)
        assert(map["c"] == null)
    }

    @Test
    fun removeTestWithLock()
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
    fun setTestWithLock()
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
    fun removeTestNoLock()
    {
        // check state
        assert(map["a"] == 3)
        assert(map["b"] == 4)
        assert(map["c"] == null)

        // try to manipulate map
        try
        {
            map["b"] = null
            throw AssertionError()
        }
        catch (ex:AssertionError)
        {
            throw ex
        }
        catch (ex:Exception)
        {
            ex.printStackTrace(System.out)
        }

        // state should not have changed
        assert(map["a"] == 3)
        assert(map["b"] == 4)
        assert(map["c"] == null)
    }

    @Test
    fun setTestNoLock()
    {
        // check state
        assert(map["a"] == 3)
        assert(map["b"] == 4)
        assert(map["c"] == null)

        // try to manipulate map
        try
        {
            map["b"] = 7
            assert(false)
            throw AssertionError()
        }
        catch (ex:AssertionError)
        {
            throw ex
        }
        catch (ex:Exception)
        {
            ex.printStackTrace(System.out)
        }

        // state should not have changed
        assert(map["a"] == 3)
        assert(map["b"] == 4)
        assert(map["c"] == null)
    }
}

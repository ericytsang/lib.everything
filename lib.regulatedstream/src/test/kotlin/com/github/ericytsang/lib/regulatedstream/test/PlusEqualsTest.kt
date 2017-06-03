package com.github.ericytsang.lib.regulatedstream.test

import org.junit.Test

class PlusEqualsTest
{
    var setCount = 0
    var hi = 0
        set(value)
        {
            field = value
            setCount++
        }

    @Test
    fun test()
    {
        hi+=0
        check(setCount == 1)
    }
}

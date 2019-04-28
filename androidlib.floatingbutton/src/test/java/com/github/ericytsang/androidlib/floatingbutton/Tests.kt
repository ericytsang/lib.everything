package com.github.ericytsang.androidlib.floatingbutton

import org.junit.Test

class Tests
{
    @Test
    fun require_range_is_inclusive()
    {
        require(0.0 in 0.0..1.0)
        require(1.0 in 0.0..1.0)
    }
}

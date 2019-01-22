package com.github.ericytsang.lib.optional

import org.junit.Test
import kotlin.test.assertEquals

class OptTest
{
    @Test
    fun opt_returns_instance_provided_in_constructor()
    {
        val opt = Opt.of(1)
        assertEquals(1,opt.opt)
    }

    @Test
    fun opt_returns_null_when_nothing_provided_to_constructor()
    {
        val opt = Opt.of<Int>()
        assertEquals(null,opt.opt)
    }
}

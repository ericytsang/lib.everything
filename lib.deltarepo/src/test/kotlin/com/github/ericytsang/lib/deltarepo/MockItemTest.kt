package com.github.ericytsang.lib.deltarepo

import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream

class MockItemTest
{
    @Test
    fun serializeable_test()
    {
        val out = ObjectOutputStream(ByteArrayOutputStream())
        out.writeObject(MockItem(0,0,false,""))
    }
}

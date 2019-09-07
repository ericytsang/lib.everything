package com.github.ericytsang.lib.optional

import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import kotlin.test.assertEquals

class SerializableOptTest
{
    @Test
    fun opt_returns_instance_provided_in_constructor()
    {
        val opt = SerializableOpt.of(1)
        assertEquals(1,opt.opt)
    }

    @Test
    fun opt_returns_null_when_nothing_provided_to_constructor()
    {
        val opt = SerializableOpt.of<Int>()
        assertEquals(null,opt.opt)
    }

    @Test
    fun some_is_serializable()
    {
        val opt:Serializable = SerializableOpt.of(1)
        ObjectOutputStream(ByteArrayOutputStream()).use {it.writeObject(opt)}
    }

    @Test
    fun none_is_serializable()
    {
        val opt:Serializable = SerializableOpt.of<Int>()
        ObjectOutputStream(ByteArrayOutputStream()).use {it.writeObject(opt)}
    }
}

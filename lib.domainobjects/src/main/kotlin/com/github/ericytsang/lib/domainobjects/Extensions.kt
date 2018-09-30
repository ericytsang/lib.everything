package com.github.ericytsang.lib.domainobjects

import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import java.io.Serializable

fun Serializable.serialize():ByteArray
{
    val baOut = ByteArrayOutputStream()
    return ObjectOutputStream(baOut).use {
        it.writeObject(this)
        baOut.toByteArray()
    }
}

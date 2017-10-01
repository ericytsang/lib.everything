package com.github.ericytsang.lib.awaitable

interface Awaitable
{
    /**
     * blocks while [updateStamp] is equal to the internal update stamp.
     * returns the value of the internal update stamp upon returning.
     */
    fun awaitUpdate(updateStamp:Long?):Long

    val updateStamp:Long
}

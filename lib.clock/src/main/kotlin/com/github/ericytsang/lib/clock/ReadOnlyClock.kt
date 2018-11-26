package com.github.ericytsang.lib.clock

interface ReadOnlyClock
{
    val tickIntervalMillis:Long
    val ticksPerSecond:Float
}

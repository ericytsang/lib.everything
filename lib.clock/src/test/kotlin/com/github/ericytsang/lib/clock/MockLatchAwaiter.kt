package com.github.ericytsang.lib.clock

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

open class MockLatchAwaiter:Function2<CountDownLatch?,Long?,Unit>
{
    override fun invoke(latch:CountDownLatch?,millis:Long?)
    {
        latch?.await(millis?:0,TimeUnit.MILLISECONDS)
    }
}

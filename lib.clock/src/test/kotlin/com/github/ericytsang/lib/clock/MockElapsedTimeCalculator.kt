package com.github.ericytsang.lib.clock

import kotlin.system.measureTimeMillis

open class MockElapsedTimeCalculator:Function1<(()->Unit)?,Long>
{
    override fun invoke(block:(()->Unit)?):Long
    {
        return measureTimeMillis(block?:{})
    }
}

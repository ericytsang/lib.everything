package com.github.ericytsang.lib.xy

data class XyBounds(
        val xBounds:ClosedFloatingPointRange<Float>,
        val yBounds:ClosedFloatingPointRange<Float>)
{
    init
    {
        require(xBounds.start <= xBounds.endInclusive)
        require(yBounds.start <= yBounds.endInclusive)
    }
}

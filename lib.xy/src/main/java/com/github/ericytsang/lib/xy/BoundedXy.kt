package com.github.ericytsang.lib.xy

class BoundedXy(
        initialPosition:Xy = Xy(0f,0f),
        initialBounds:XyBounds = XyBounds(0f..0f,0f..0f))
{
    /**
     * the lower and upper bounds that [position]
     * may be assigned to.
     */
    var bounds = initialBounds
        set(value)
        {
            field = value
            position = position
        }

    /**
     * some [Xy] coordinate that is always within [bounds]
     */
    var position = initialPosition
        set(value)
        {
            field = Xy(
                    value.x.coerceIn(bounds.xBounds),
                    value.y.coerceIn(bounds.yBounds))
        }
}

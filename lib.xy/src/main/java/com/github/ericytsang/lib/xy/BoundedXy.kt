package com.github.ericytsang.lib.xy

import com.github.ericytsang.lib.prop.DataProp
import com.github.ericytsang.lib.prop.ReadOnlyProp
import com.github.ericytsang.lib.prop.value

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
            _onChanged.value = Unit
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
            _onChanged.value = Unit
        }

    /**
     * used to notify listeners that a property has changed.
     */
    private val _onChanged = DataProp(Unit)
    val onChanged:ReadOnlyProp<Unit,Unit> = _onChanged
}

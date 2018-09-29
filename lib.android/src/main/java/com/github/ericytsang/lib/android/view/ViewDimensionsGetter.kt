package com.github.ericytsang.lib.android.view

class ViewDimensionsGetter(
        val parameters:ViewDimensions)
    :ViewProperty.Visitor<Int>
{
    override fun visit(receiver:ViewProperty.Width):Int
    {
        return parameters.width
    }

    override fun visit(receiver:ViewProperty.Height):Int
    {
        return parameters.height
    }
}
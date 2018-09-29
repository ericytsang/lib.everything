package com.github.ericytsang.lib.android.view

import android.view.ViewGroup

class LayoutParamsGetter(
        val parameters:ViewGroup.LayoutParams)
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
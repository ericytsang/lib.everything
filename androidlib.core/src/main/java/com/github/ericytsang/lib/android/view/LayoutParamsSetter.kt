package com.github.ericytsang.androidlib.view

import android.view.ViewGroup

class LayoutParamsSetter(
        val layoutParams:ViewGroup.LayoutParams,
        val newValue:Int)
    :ViewProperty.Visitor<Unit>
{
    override fun visit(receiver:ViewProperty.Width)
    {
        layoutParams.width = newValue
    }

    override fun visit(receiver:ViewProperty.Height)
    {
        layoutParams.height = newValue
    }
}
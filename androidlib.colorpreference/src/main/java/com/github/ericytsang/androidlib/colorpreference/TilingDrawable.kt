package com.github.ericytsang.androidlib.colorpreference

import android.graphics.Canvas
import android.graphics.Point
import android.graphics.Rect
import android.graphics.drawable.Drawable
import androidx.appcompat.graphics.drawable.DrawableWrapper
import kotlin.math.roundToInt

// todo: separate android library
class TilingDrawable(
        drawable:Drawable,
        val repeatMode:RepeatMode)
    :DrawableWrapper(drawable)
{

    private var callbackEnabled = true

    override fun draw(canvas:Canvas)
    {
        callbackEnabled = false
        val bounds = bounds
        val wrappedDrawable = wrappedDrawable

        val width = wrappedDrawable.intrinsicWidth
        val height = wrappedDrawable.intrinsicHeight
        val startingPoint = repeatMode.startingPoint(bounds,Point(width,height))
        var x = startingPoint.x
        while (x < bounds.right+width-1)
        {
            var y = startingPoint.y
            while (y < bounds.bottom+height-1)
            {
                wrappedDrawable.setBounds(x,y,x+width,y+height)
                wrappedDrawable.draw(canvas)
                y += height
            }
            x += width
        }
        callbackEnabled = true
    }

    override fun onBoundsChange(bounds:Rect) = Unit

    /**
     * {@inheritDoc}
     */
    override fun invalidateDrawable(who:Drawable)
    {
        if (callbackEnabled)
        {
            super.invalidateDrawable(who)
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun scheduleDrawable(who:Drawable,what:Runnable,`when`:Long)
    {
        if (callbackEnabled)
        {
            super.scheduleDrawable(who,what,`when`)
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun unscheduleDrawable(who:Drawable,what:Runnable)
    {
        if (callbackEnabled)
        {
            super.unscheduleDrawable(who,what)
        }
    }

    enum class RepeatMode
    {
        FROM_TOP_LEFT
        {
            override fun startingPoint(bounds:Rect,dimensions:Point):Point
            {
                return Point(bounds.left,bounds.top)
            }
        },
        FROM_CENTER
        {
            override fun startingPoint(bounds:Rect,dimensions:Point):Point
            {
                return Point(
                        ((bounds.right-bounds.left)/2f-dimensions.x/2f-(bounds.right-bounds.left)/2f).roundToInt(),
                        ((bounds.bottom-bounds.top)/2f-dimensions.x/2f-(bounds.bottom-bounds.top)/2f).roundToInt())
            }
        };
        abstract fun startingPoint(bounds:Rect,dimensions:Point):Point
    }
}
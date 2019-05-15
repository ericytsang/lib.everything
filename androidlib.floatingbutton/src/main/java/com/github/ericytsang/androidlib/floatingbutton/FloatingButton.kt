package com.github.ericytsang.androidlib.floatingbutton

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.RelativeLayout
import com.github.ericytsang.androidlib.core.HvOrientation
import com.github.ericytsang.androidlib.core.Orientation
import com.github.ericytsang.androidlib.core.layoutInflater
import com.github.ericytsang.androidlib.core.realScreenDimensionsForOrientation
import com.github.ericytsang.androidlib.core.windowManager
import com.github.ericytsang.androidlib.screenmeasurer.OrientationChange
import com.github.ericytsang.androidlib.touchlisteners.TouchHandler
import com.github.ericytsang.lib.closeablegroup.CloseableGroup
import com.github.ericytsang.lib.prop.ReadOnlyProp
import com.github.ericytsang.lib.prop.listen
import com.github.ericytsang.lib.prop.value
import com.github.ericytsang.lib.xy.BoundedXy
import com.github.ericytsang.lib.xy.Xy
import com.github.ericytsang.lib.xy.XyBounds
import java.io.Closeable
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * this is a floating button that has multiple strategies:
 * * optional have tap/long press actions
 * * optional drag to move
 */
class FloatingButton(
        val rootView:View,
        val strategy:Strategy,

        /**
         * x and y are [Float] values between 0, and 1.
         * 0.0 for x/y -> button is on the very left/top of the screen.
         * 1.0 for x/y -> button is on the very right/bottom of the screen.
         * position is calculated as if device is in portrait orientation.
         */
        initialPositionScalar:Xy = Xy(0.5f,0.5f))
    :Closeable
{
    // validate input
    init
    {
        // validate initialPositionScalar
        require(initialPositionScalar.x in 0.0..1.0)
        require(initialPositionScalar.y in 0.0..1.0)
    }

    private val thingsToClose = CloseableGroup()

    override fun close() = thingsToClose.close()

    // floating point x and y position of the floating button
    private val position = run()
    {
        val measurements = rootView.context.windowManager.defaultDisplay.realScreenDimensionsForOrientation(HvOrientation.VERTICAL)
        val screenBounds = strategy.screenDimensions.value.dimensions.screenBoundsIncludingNavBar
        BoundedXy(
                Xy(
                        measurements.w*initialPositionScalar.x,
                        measurements.h*initialPositionScalar.y),
                XyBounds(
                        screenBounds.xBounds.run {start..endInclusive-strategy.dimensions.value.x},
                        screenBounds.yBounds.run {start..endInclusive-strategy.dimensions.value.y}))
    }

    // convert position when orientation changes
    init
    {
        val oldDimensions = OldValueHolder(strategy.screenDimensions.value)
        thingsToClose += listOf(strategy.screenDimensions).listen()
        {
            val newDimensions = strategy.screenDimensions.value

            // convert position to scalar coordinates
            oldDimensions.doThenSet(newDimensions)
            {
                val newPosition = transformCoordinate(
                        oldDimensions.oldValue.orientation,
                        newDimensions.orientation,
                        position.position,
                        newDimensions.dimensions.screenBoundsIncludingNavBar)
                position.bounds = newDimensions.dimensions.screenBoundsIncludingNavBar
                position.position = newPosition
            }
        }
    }

    // layout parameters used to attach the button to the window manager
    private val layoutParams = WindowManager.LayoutParams().apply()
    {
        type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        format = PixelFormat.TRANSLUCENT
        flags = flags
                .or(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
                .or(WindowManager.LayoutParams.FLAG_LAYOUT_IN_OVERSCAN)
                .or(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)
                .or(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        gravity = Gravity.TOP or Gravity.LEFT
    }
        get()
        {
            field.width = strategy.dimensions.value.x.roundToInt()
            field.height = strategy.dimensions.value.y.roundToInt()
            field.x = position.position.x.roundToInt()
            field.y = position.position.y.roundToInt()
            return field
        }

    // add this to the window
    init
    {
        rootView.context.windowManager.addView(rootView,layoutParams)
        thingsToClose += Closeable()
        {
            rootView.context.windowManager.removeView(rootView)
        }
    }

    // size of floating button matches property
    // position of floating button matches property
    init
    {
        thingsToClose += listOf(strategy.dimensions,position.onChanged).listen()
        {
            rootView.context.windowManager.updateViewLayout(rootView,layoutParams)
        }
    }

    interface Strategy
    {
        val dimensions:ReadOnlyProp<Unit,Xy>
        val screenDimensions:ReadOnlyProp<Unit,OrientationChange>
    }

    // manages the velocity of the floating button as it is decelerating to a stop after being flung...
    private val flingAnimator = object
    {
        private val ongoingValueAnimators = mutableSetOf<ValueAnimator>()

        init
        {
            thingsToClose += Closeable()
            {
                endAllOngoingAnimations()
            }
        }

        /**
         * stops any ongoing flings, and immediately makes the floating button
         * fling using the given [flingVelocity].
         */
        fun setFling(flingVelocity:Xy)
        {
            val initialVelocity = sqrt(flingVelocity.squaredDistance)
            val stoppingDuration = initialVelocity.div(0.05f).toLong()
            val initialPosition = position.position
            endAllOngoingAnimations()
            ongoingValueAnimators += ValueAnimator
                    .ofFloat(1f,0f)
                    .setDuration(stoppingDuration)
                    .apply {addUpdateListener()
                    {
                        val animatedValue = (it.animatedValue as Float)
                        val elapsedTime = (1f-animatedValue)*stoppingDuration
                        val currentVelocity = flingVelocity*animatedValue
                        val distanceFromInitialPosition = (flingVelocity+currentVelocity)*elapsedTime/2f
                        position.position = initialPosition+distanceFromInitialPosition
                    }}
                    .apply {start()}
        }

        /**
         * ends all ongoing animations
         */
        private fun endAllOngoingAnimations()
        {
            ongoingValueAnimators.forEach {it.cancel()}
            ongoingValueAnimators.clear()
        }
    }

    // drag action that can be injected into touch listeners
    private val dragToMoveButton = object:TouchHandler.DragAction
    {
        override fun dragStart(firstPosition:Xy,eventTime:Long):TouchHandler.DragAction.DragContinuation
        {
            flingAnimator.setFling(Xy.ZERO)
            return DragToMoveButtonDragContinuation()
        }
    }

    private inner class DragToMoveButtonDragContinuation
        :TouchHandler.DragAction.DragContinuation
    {
        override fun dragContinue(currentPosition:Xy,deltaPosition:Xy,eventTime:Long)
        {
            position.position += deltaPosition
        }

        override fun dragEnd(flingVelocity:Xy,eventTime:Long)
        {
            flingAnimator.setFling(flingVelocity)
        }
    }

    private class OldValueHolder<T>(_oldValue:T)
    {
        var oldValue = _oldValue
            private set

        fun doAndSetIfNotEq(newValue:T,block:()->Unit)
        {
            if (oldValue != newValue)
            {
                block()
                oldValue = newValue
            }
        }

        fun doThenSet(newValue:T,block:()->Unit)
        {
            block()
            oldValue = newValue
        }
    }

    private fun transformCoordinate(
            oldScreenOrientation:Orientation,
            newScreenOrientation:Orientation,
            position:Xy,
            positionBounds:XyBounds):Xy
    {
        val offsetToPositionBoundsOrigin = positionBounds.run {Xy(xBounds.start,yBounds.start)}
        val offsetPosition = position-offsetToPositionBoundsOrigin
        val w = positionBounds.xBounds.run {endInclusive-start}
        val h = positionBounds.yBounds.run {endInclusive-start}
        val transformedPosition = when (oldScreenOrientation)
        {
            Orientation.REGULAR_PORTRAIT -> when (newScreenOrientation)
            {
                Orientation.REGULAR_PORTRAIT -> offsetPosition
                Orientation.REVERSE_PORTRAIT -> offsetPosition.run {copy(x = w-x,y = h-y)}
                Orientation.REGULAR_LANDSCAPE -> offsetPosition.run {copy(x = y,y = h-x)}
                Orientation.REVERSE_LANDSCAPE -> offsetPosition.run {copy(x = w-y,y = x)}
            }
            Orientation.REVERSE_PORTRAIT -> when (newScreenOrientation)
            {
                Orientation.REVERSE_PORTRAIT -> offsetPosition
                Orientation.REGULAR_PORTRAIT -> offsetPosition.run {copy(x = w-x,y = h-y)}
                Orientation.REVERSE_LANDSCAPE -> offsetPosition.run {copy(x = y,y = h-x)}
                Orientation.REGULAR_LANDSCAPE -> offsetPosition.run {copy(x = w-y,y = x)}
            }
            Orientation.REGULAR_LANDSCAPE -> when (newScreenOrientation)
            {
                Orientation.REGULAR_LANDSCAPE -> offsetPosition
                Orientation.REVERSE_LANDSCAPE -> offsetPosition.run {copy(x = w-x,y = h-y)}
                Orientation.REVERSE_PORTRAIT -> offsetPosition.run {copy(x = y,y = h-x)}
                Orientation.REGULAR_PORTRAIT -> offsetPosition.run {copy(x = w-y,y = x)}
            }
            Orientation.REVERSE_LANDSCAPE -> when (newScreenOrientation)
            {
                Orientation.REVERSE_LANDSCAPE -> offsetPosition
                Orientation.REGULAR_LANDSCAPE -> offsetPosition.run {copy(x = w-x,y = h-y)}
                Orientation.REGULAR_PORTRAIT -> offsetPosition.run {copy(x = y,y = h-x)}
                Orientation.REVERSE_PORTRAIT -> offsetPosition.run {copy(x = w-y,y = x)}
            }
        }
        return transformedPosition+offsetToPositionBoundsOrigin
    }
}

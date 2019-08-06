package com.github.ericytsang.androidlib.floatingbutton

import android.animation.ValueAnimator
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import com.github.ericytsang.androidlib.core.HvOrientation
import com.github.ericytsang.androidlib.core.Orientation
import com.github.ericytsang.androidlib.core.realScreenDimensionsForOrientation
import com.github.ericytsang.androidlib.core.windowManager
import com.github.ericytsang.androidlib.screenmeasurer.OrientationChange
import com.github.ericytsang.androidlib.touchlisteners.TouchHandler
import com.github.ericytsang.lib.closeablegroup.CloseableGroup
import com.github.ericytsang.lib.prop.DataProp
import com.github.ericytsang.lib.prop.ReadOnlyProp
import com.github.ericytsang.lib.prop.listen
import com.github.ericytsang.lib.prop.value
import com.github.ericytsang.lib.xy.BoundedXy
import com.github.ericytsang.lib.xy.Xy
import com.github.ericytsang.lib.xy.XyBounds
import java.io.Closeable
import kotlin.math.roundToInt
import kotlin.math.sqrt

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

    /**
     * [thingsToClose]: resources allocated during initialization that are to be
     * released upon [FloatingButton.close].
     */
    private val thingsToClose = CloseableGroup()
    override fun close() = thingsToClose.close()

    /**
     * [position]: floating point x and y position of the floating button
     */
    private val position = run()
    {
        val measurements = rootView.context.windowManager.defaultDisplay.realScreenDimensionsForOrientation(HvOrientation.VERTICAL)
        val screenBounds = strategy.screenDimensions.value.dimensions.screenBoundsIncludingNavBar
        BoundedXy(
                Xy(
                        measurements.w*initialPositionScalar.x-strategy.dimensions.value.x/2,
                        measurements.h*initialPositionScalar.y-strategy.dimensions.value.y/2),
                XyBounds(
                        screenBounds.xBounds.run {start..endInclusive-strategy.dimensions.value.x},
                        screenBounds.yBounds.run {start..endInclusive-strategy.dimensions.value.y}))
    }

    /**
     * allow access to the scalar position of this [FloatingButton].
     */
    val portraitModePositionAsScalar:ReadOnlyProp<Unit,Xy> get() = _portraitModePositionAsScalar
    private val _portraitModePositionAsScalar = DataProp(initialPositionScalar)

    init
    {
        object
        {
            /**
             * [positionAsScalar]: allows access to [position].[BoundedXy.position] in a way where
             * - (0,0) is the top left of the screen,
             * - and (1,1) is the bottom right.
             */
            var positionAsScalar:Xy
                get() = Xy(
                        position.bounds.xBounds.run {(position.position.x-start)/(endInclusive-start)},
                        position.bounds.yBounds.run {(position.position.y-start)/(endInclusive-start)})
                set(value)
                {
                    position.position = Xy(
                            position.bounds.xBounds.run {value.x*(endInclusive-start)+start},
                            position.bounds.yBounds.run {value.y*(endInclusive-start)+start})
                }

            /**
             * [oldDimensions]: dimension that the position is calibrated for.
             * e.g. position was set while the phone's is in landscape mode;
             * [oldDimensions].[OrientationChange.dimensions] == [Orientation.REGULAR_LANDSCAPE].
             * but the phone is now in [Orientation.REGULAR_PORTRAIT].
             */
            val oldDimensions = OldValueHolder(strategy.screenDimensions.value
                    .copy(orientation = Orientation.REGULAR_PORTRAIT))

            // convert position when orientation or dimension changes.
            init
            {
                thingsToClose.chainedAddCloseables()
                {
                    thingsToClose->
                    thingsToClose += listOf(strategy.screenDimensions,strategy.dimensions).listen()
                    {
                        val newDimensions = strategy.screenDimensions.value

                        // convert position to scalar coordinates
                        oldDimensions.doThenSet(newDimensions)
                        {
                            val newPosition = transformCoordinate(
                                    oldDimensions.oldValue.orientation,
                                    newDimensions.orientation,
                                    positionAsScalar,
                                    XyBounds(0f..1f,0f..1f))
                            val screenBounds = newDimensions.dimensions.screenBoundsIncludingNavBar
                            position.bounds = XyBounds(
                                    screenBounds.xBounds.run {start..endInclusive-strategy.dimensions.value.x},
                                    screenBounds.yBounds.run {start..endInclusive-strategy.dimensions.value.y})
                            positionAsScalar = newPosition
                        }
                    }
                }
            }

            // save position when it is updated
            init
            {
                thingsToClose.chainedAddCloseables()
                {
                    thingsToClose ->
                    thingsToClose += listOf(position.onChanged).listen()
                    {
                        _portraitModePositionAsScalar.value = transformCoordinate(
                                oldDimensions.oldValue.orientation,
                                Orientation.REGULAR_PORTRAIT,
                                positionAsScalar,
                                XyBounds(0f..1f,0f..1f))
                    }
                }
            }
        }
    }

    init
    {
        object
        {
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

            private fun layoutParams(position:Xy,dimensions:Xy):WindowManager.LayoutParams
            {
                layoutParams.width = dimensions.x.roundToInt()
                layoutParams.height = dimensions.y.roundToInt()
                layoutParams.x = position.x.roundToInt()
                layoutParams.y = position.y.roundToInt()
                return layoutParams
            }

            // add floating button to the window & remove it upon [FloatingButton.close].
            init
            {
                thingsToClose.chainedAddCloseables()
                {
                    thingsToClose ->
                    rootView.context.windowManager.addView(rootView,layoutParams(position.position,strategy.dimensions.value))
                    thingsToClose += Closeable()
                    {
                        rootView.context.windowManager.removeView(rootView)
                    }
                }
            }

            // size of floating button matches property
            // position of floating button matches property
            init
            {
                thingsToClose.chainedAddCloseables()
                {
                    thingsToClose ->
                    thingsToClose += listOf(strategy.dimensions,position.onChanged).listen()
                    {
                        rootView.context.windowManager.updateViewLayout(rootView,layoutParams(position.position,strategy.dimensions.value))
                    }
                }
            }
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
            thingsToClose.chainedAddCloseables()
            {
                thingsToClose ->
                thingsToClose += Closeable()
                {
                    endAllOngoingAnimations()
                }
            }
        }

        /**
         * stops any ongoing flings, and immediately makes the floating button
         * fling using the given [flingVelocity].
         */
        fun setFling(flingVelocity:TouchHandler.DragAction.Velocity)
        {
            val initialVelocityXy = flingVelocity.deltaPosition/flingVelocity.deltaTimeMillis.toFloat().coerceAtLeast(0.0001f)
            val initialVelocity = sqrt(initialVelocityXy.squaredDistance)
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
                        val currentVelocity = initialVelocityXy*animatedValue
                        val distanceFromInitialPosition = (initialVelocityXy+currentVelocity)*elapsedTime/2f
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
    val dragToMoveButton = object:TouchHandler.DragAction
    {
        override fun dragStart(firstPosition:Xy,eventTime:Long):TouchHandler.DragAction.DragContinuation
        {
            flingAnimator.setFling(TouchHandler.DragAction.Velocity(Xy.ZERO,0))
            return DragToMoveButtonDragContinuation()
        }

        private inner class DragToMoveButtonDragContinuation
            :TouchHandler.DragAction.DragContinuation
        {
            override fun dragContinue(currentPosition:Xy,deltaPosition:Xy,eventTime:Long)
            {
                position.position += deltaPosition
            }

            override fun dragEnd(flingVelocity:TouchHandler.DragAction.Velocity,eventTime:Long)
            {
                flingAnimator.setFling(flingVelocity)
            }
        }
    }

    private class OldValueHolder<T>(_oldValue:T)
    {
        var oldValue = _oldValue
            private set

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

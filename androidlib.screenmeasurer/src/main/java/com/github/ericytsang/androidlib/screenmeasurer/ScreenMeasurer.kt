package com.github.ericytsang.androidlib.screenmeasurer

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.graphics.PointF
import android.view.Gravity
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import com.github.ericytsang.androidlib.core.DoLog
import com.github.ericytsang.androidlib.core.realScreenDimensionsForOrientation
import com.github.ericytsang.androidlib.core.screenOrientation
import com.github.ericytsang.androidlib.core.windowManager
import com.github.ericytsang.lib.prop.DataProp
import com.github.ericytsang.lib.prop.value
import com.github.ericytsang.lib.xy.XyBounds
import java.io.Closeable

/**
 * gets the maximum usable y coordinate in the screen coordinate system that
 * the cursor should be able to go before it is overlapping with the
 * system navigation bar, and notches....this accounts for notches too.
 */
class ScreenMeasurer(
        val context:AccessibilityService)
    :
        BroadcastReceiver(),
        Closeable
{
    private val bottomRightView = View(context)
    private val topLeftView = View(context)

    val orientation = DataProp(OrientationChange(
            context.windowManager.defaultDisplay.screenOrientation,
            computeScreenDimensions()))

    private val layoutChangeListener = object:ViewTreeObserver.OnGlobalLayoutListener,DoLog
    {
        override fun onGlobalLayout()
        {
            val newOrientation = context.windowManager.defaultDisplay.screenOrientation
            val screenDimens = computeScreenDimensions()

            orientation.value = OrientationChange(newOrientation,screenDimens)
        }
    }

    init
    {
        // add views
        //@SuppressLint("RtlHardcoded") // we use Gravity.RIGHT, because we are interested in the x coordinate, not the layout...
        context.windowManager.addView(bottomRightView,layoutParams(Gravity.RIGHT or Gravity.BOTTOM))
        //@SuppressLint("RtlHardcoded") // we use Gravity.LEFT, because we are interested in the x coordinate, not the layout...
        context.windowManager.addView(topLeftView,layoutParams(Gravity.LEFT or Gravity.TOP))

        // add layout change listeners to views to capture orientation changes
        bottomRightView.viewTreeObserver.addOnGlobalLayoutListener(layoutChangeListener)
        topLeftView.viewTreeObserver.addOnGlobalLayoutListener(layoutChangeListener)

        // add screen state listener because the screen is set back to
        // portrait mode when it is turned off.. (at least on my device)
        // without notifying the layout change listeners :/
        context.registerReceiver(this,IntentFilter(Intent.ACTION_SCREEN_ON))
    }

    override fun close()
    {
        context.unregisterReceiver(this)

        bottomRightView.viewTreeObserver.removeOnGlobalLayoutListener(layoutChangeListener)
        topLeftView.viewTreeObserver.removeOnGlobalLayoutListener(layoutChangeListener)

        context.windowManager.removeViewImmediate(bottomRightView)
        context.windowManager.removeViewImmediate(topLeftView)
    }

    fun computeScreenDimensions():ScreenDimensions
    {
        val realScreenSize = context.windowManager.defaultDisplay
                .run {realScreenDimensionsForOrientation(screenOrientation.hvOrientation)}
        val bottomRight = IntArray(2)
                .also {bottomRightView.getLocationOnScreen(it)}
                .let {PointF(it[0].toFloat(),it[1].toFloat())}
        val topLeft = IntArray(2)
                .also {topLeftView.getLocationOnScreen(it)}
                .let {PointF(it[0].toFloat(),it[1].toFloat())}
        val screenBoundsExcludingNavBar = XyBounds(
                0f..(bottomRight.x-topLeft.x),
                0f..(bottomRight.y-topLeft.y))
        val screenBoundsIncludingNavBar = XyBounds(
                (-topLeft.x)..(realScreenSize.w-topLeft.x),
                (-topLeft.y)..(realScreenSize.h-topLeft.y))
        return ScreenDimensions(
                realScreenSize,
                screenBoundsExcludingNavBar,
                screenBoundsIncludingNavBar)
    }

    // compute layout change when screen turns on too....
    override fun onReceive(context:Context?,intent:Intent?)
    {
        if (intent?.action == Intent.ACTION_SCREEN_ON)
        {
            layoutChangeListener.onGlobalLayout()
        }
    }

    private fun layoutParams(gravity:Int) = WindowManager.LayoutParams().also()
    {
        layoutParameters ->
        layoutParameters.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        layoutParameters.format = PixelFormat.TRANSLUCENT
        layoutParameters.flags = layoutParameters.flags
                .or(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)
                .or(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
                .or(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
                .or(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        layoutParameters.width = 2
        layoutParameters.height = 2
        layoutParameters.gravity = gravity
    }
}

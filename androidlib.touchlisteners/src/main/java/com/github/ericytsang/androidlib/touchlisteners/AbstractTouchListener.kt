package com.github.ericytsang.androidlib.touchlisteners

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View

class AbstractTouchListener(
        private val listener:Listener)
    :View.OnTouchListener
{
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v:View,event:MotionEvent):Boolean
    {
        return when (event.action)
        {
            MotionEvent.ACTION_DOWN ->
            {
                listener.touchBegin(v,event)
                true
            }
            MotionEvent.ACTION_MOVE ->
            {
                listener.touchContinue(v,event)
                true
            }
            MotionEvent.ACTION_CANCEL,
            MotionEvent.ACTION_UP ->
            {
                listener.touchEnd(v,event)
                true
            }
            else ->
            {
                false
            }
        }
    }

    interface Listener
    {
        fun touchBegin(v:View,event:MotionEvent)
        fun touchContinue(v:View,event:MotionEvent)
        fun touchEnd(v:View,event:MotionEvent)
    }
}

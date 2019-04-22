package com.github.ericytsang.androidlib.touchlisteners

import android.view.MotionEvent
import android.view.View
import com.github.ericytsang.lib.optional.Opt
import com.github.ericytsang.lib.xy.Xy

class TouchHandler(
        val touchSessionFactory:PrimaryPointerFilterTouchListener.Listener)
    :PrimaryPointerFilterTouchListener.Listener
{
    override fun touchBegin(v:View,event:MotionEvent):PrimaryPointerFilterTouchListener.Listener.TouchSession
         = touchSessionFactory.touchBegin(v,event)

    data class Actions(
            val tapAction:Opt<OneShotAction>,
            val doubleTapAction:Opt<OneShotAction>,
            val longTapAction:Opt<OneShotAction>,
            val dragAction:Opt<DragAction>,
            val doubleTapDragAction:Opt<DragAction>,
            val longTapDragAction:Opt<DragAction>)

    interface OneShotAction
    {
        fun doAction(v:View)

        class OnClickListenerAdapter(
                val strategy:View.OnClickListener)
            :OneShotAction
        {
            override fun doAction(v:View)
            {
                strategy.onClick(v)
            }
        }

        class LambdaAdapter(
                val strategy:(View)->Unit)
            :OneShotAction
        {
            override fun doAction(v:View)
            {
                strategy(v)
            }
        }
    }

    interface DragAction
    {
        fun dragStart(firstPosition:Xy,eventTime:Long):DragContinuation
        interface DragContinuation
        {
            fun dragContinue(currentPosition:Xy,deltaPosition:Xy,eventTime:Long)
            fun dragEnd(flingVelocity:Xy,eventTime:Long)
        }
    }
}

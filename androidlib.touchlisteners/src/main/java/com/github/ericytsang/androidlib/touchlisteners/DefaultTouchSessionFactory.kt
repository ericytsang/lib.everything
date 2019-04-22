package com.github.ericytsang.androidlib.touchlisteners

import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import com.github.ericytsang.androidlib.core.postOnUiThread
import com.github.ericytsang.lib.optional.Opt
import com.github.ericytsang.lib.xy.Xy
import java.io.Serializable
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class DefaultTouchSessionFactory(
        val actions:TouchHandler.Actions,
        val timeouts:Timeouts,
        val feedbackListener:FeedbackListener = VIBRATE_FEEDBACK_LISTENER)
    :PrimaryPointerFilterTouchListener.Listener
{
    override fun touchBegin(v:View,event:MotionEvent):PrimaryPointerFilterTouchListener.Listener.TouchSession
    {
        return touchSessionFactory(this).touchBegin(v,event)
    }

    private var touchSessionFactory = fun(context:DefaultTouchSessionFactory):TouchSessionFactory
    {
        return TouchSessionFactory.New(context,actions,timeouts,feedbackListener)
    }

    companion object
    {
        val NULL_FEEDBACK_LISTENER = object:FeedbackListener
        {}
        val VIBRATE_FEEDBACK_LISTENER = object:FeedbackListener
        {
            override fun onLongClickThresholdReached(v:View)
            {
                v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            }

            override fun onClicked(v:View)
            {
                v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            }
        }
    }

    interface FeedbackListener
    {
        fun onLongClickPointerReleasedAndActionIsDispatched(v:View) = Unit
        fun onLongClickThresholdReached(v:View) = Unit
        fun onClicked(v:View) = Unit
    }

    interface Timeouts:Serializable
    {
        val longPressTimeout:Long
        val doubleTapTimeout:Long
    }

    data class SimpleTimeouts(
            override val longPressTimeout:Long,
            override val doubleTapTimeout:Long)
        :Timeouts

    class OsTimeouts:Timeouts
    {
        override val longPressTimeout:Long = ViewConfiguration.getLongPressTimeout().toLong()
        override val doubleTapTimeout:Long = ViewConfiguration.getDoubleTapTimeout().toLong()
    }

    private sealed class TouchSessionFactory
        :PrimaryPointerFilterTouchListener.Listener
    {

        class New(
                val context:DefaultTouchSessionFactory,
                val actions:TouchHandler.Actions,
                val timeouts:Timeouts,
                val feedbackListener:FeedbackListener)
            :TouchSessionFactory()
        {
            override fun touchBegin(v:View,event:MotionEvent):PrimaryPointerFilterTouchListener.Listener.TouchSession
            {
                return TouchSession.Down(
                        context,
                        actions,
                        ViewConfiguration.get(v.context),
                        timeouts,
                        Xy(event.rawX,event.rawY),
                        feedbackListener)
            }
        }

        class AfterFirstTapOfDoubleTap(
                val view:View,
                val context:DefaultTouchSessionFactory,
                val actions:TouchHandler.Actions,
                val timeouts:Timeouts,
                val feedbackListener:FeedbackListener,
                val touchSessionForDoubleTap:(v:View,event:MotionEvent)->TouchSession)
            :TouchSessionFactory()
        {
            val lock = ReentrantLock()
            var actionWasDone = false

            init
            {
                postOnUiThread(timeouts.doubleTapTimeout)
                {
                    lock.withLock()
                    {
                        if (!actionWasDone)
                        {
                            actionWasDone = true
                            val action = actions.tapAction.opt
                            if (action != null)
                            {
                                action.doAction(view)
                                feedbackListener.onClicked(view)
                            }
                        }
                    }
                }
            }

            override fun touchBegin(v:View,event:MotionEvent):PrimaryPointerFilterTouchListener.Listener.TouchSession
            {
                context.touchSessionFactory = {context -> New(context,actions,timeouts,feedbackListener)}
                return lock.withLock()
                {
                    if (!actionWasDone)
                    {
                        actionWasDone = true
                        touchSessionForDoubleTap(v,event)
                    }
                    else
                    {
                        New(context,actions,timeouts,feedbackListener).touchBegin(v,event)
                    }
                }
            }
        }
    }

    private sealed class TouchSession
        :PrimaryPointerFilterTouchListener.Listener.TouchSession
    {
        class Down(
                val context:DefaultTouchSessionFactory,
                val actions:TouchHandler.Actions,
                val viewConfiguration:ViewConfiguration,
                val timeouts:Timeouts,
                val downPosition:Xy,
                val feedbackListener:FeedbackListener)
            :TouchSession()
        {
            private val isLongPressTimeoutElapsed:(v:View,event:MotionEvent)->TouchSession? = run()
            {
                fun takeIfLongPressTimeoutExceeded(touchSessionFactory:(v:View,event:MotionEvent)->TouchSession):(v:View,event:MotionEvent)->TouchSession?
                {
                    val longPressTimeout = timeouts.longPressTimeout
                    return {
                        v:View,event:MotionEvent ->
                        if ((event.eventTime-event.downTime) >= longPressTimeout)
                        {
                            feedbackListener.onLongClickThresholdReached(v)
                            touchSessionFactory(v,event)
                        }
                        else
                        {
                            null
                        }
                    }
                }

                when(actions.longTapAction)
                {
                    is Opt.Some -> when(actions.longTapDragAction)
                    {
                        is Opt.Some ->
                        {
                            takeIfLongPressTimeoutExceeded()
                            {
                                v,_ ->
                                AwaitUpOrDrag(
                                        actions.longTapAction.opt.alsoDo {feedbackListener.onLongClickPointerReleasedAndActionIsDispatched(v)},
                                        actions.longTapDragAction.opt.alsoDo {feedbackListener.onLongClickPointerReleasedAndActionIsDispatched(v)},
                                        viewConfiguration,
                                        downPosition)
                            }
                        }
                        is Opt.None ->
                        {
                            takeIfLongPressTimeoutExceeded()
                            {
                                v,_ ->
                                AwaitUp(
                                        actions.longTapAction.opt.alsoDo {feedbackListener.onLongClickPointerReleasedAndActionIsDispatched(v)})
                            }
                        }
                    }
                    is Opt.None -> when(actions.longTapDragAction)
                    {
                        is Opt.Some ->
                        {
                            takeIfLongPressTimeoutExceeded()
                            {
                                v,event ->
                                Dragging(
                                        actions.longTapDragAction.opt.alsoDo {feedbackListener.onLongClickPointerReleasedAndActionIsDispatched(v)},
                                        Xy(event.rawX,event.rawY),
                                        event.eventTime)
                            }
                        }
                        is Opt.None -> {_,_-> null}
                    }
                }
            }

            private val isDragTouchSlopExceeded:(event:MotionEvent)->TouchSession? = run()
            {
                fun takeIfDragTouchSlopExceeded(touchSessionFactory:(event:MotionEvent)->TouchSession):(event:MotionEvent)->TouchSession?
                {
                    val squaredTouchSlop = listOf(
                            actions.tapAction,
                            actions.doubleTapAction,
                            actions.doubleTapDragAction,
                            actions.longTapAction,
                            actions.longTapDragAction)
                            .map {
                                optional->
                                when(optional)
                                {
                                    is Opt.Some -> viewConfiguration.scaledTouchSlop.let {it*it}
                                    is Opt.None -> 0
                                }
                            }
                            .max()!!

                    return {
                        event:MotionEvent ->
                        if (Xy(event.rawX,event.rawY).minus(downPosition).squaredDistance >= squaredTouchSlop)
                        {
                            touchSessionFactory(event)
                        }
                        else
                        {
                            null
                        }
                    }
                }

                when(actions.dragAction)
                {
                    is Opt.Some ->
                    {
                        takeIfDragTouchSlopExceeded()
                        {
                            event->
                            Dragging(
                                    actions.dragAction.opt,
                                    Xy(event.rawX,event.rawY),
                                    event.eventTime)
                        }
                    }
                    is Opt.None -> {{null}}
                }
            }

            override fun touchContinue(v:View,event:MotionEvent):PrimaryPointerFilterTouchListener.Listener.TouchSession
            {
                return null
                        ?:isLongPressTimeoutElapsed(v,event)
                        ?:isDragTouchSlopExceeded(event)
                        ?:this
            }

            override fun touchEnd(v:View,event:MotionEvent)
            {
                context.touchSessionFactory = when(actions.doubleTapAction)
                {
                    is Opt.Some -> when(actions.doubleTapDragAction)
                    {
                        is Opt.Some ->
                        {
                            feedbackListener.onClicked(v)
                            TouchSessionFactory.AfterFirstTapOfDoubleTap(v,context,actions,timeouts,feedbackListener)
                            {_:View,_:MotionEvent ->
                                AwaitUpOrDrag(
                                        actions.doubleTapAction.opt,
                                        actions.doubleTapDragAction.opt,
                                        viewConfiguration,
                                        downPosition)
                            }.let {{_:DefaultTouchSessionFactory ->it}}
                        }
                        is Opt.None ->
                        {
                            feedbackListener.onClicked(v)
                            TouchSessionFactory.AfterFirstTapOfDoubleTap(v,context,actions,timeouts,feedbackListener)
                            {_:View,_:MotionEvent ->
                                AwaitUp(actions.doubleTapAction.opt)
                            }.let {{_:DefaultTouchSessionFactory ->it}}
                        }
                    }
                    is Opt.None -> when(actions.doubleTapDragAction)
                    {
                        is Opt.Some ->
                        {
                            feedbackListener.onClicked(v)
                            TouchSessionFactory.AfterFirstTapOfDoubleTap(v,context,actions,timeouts,feedbackListener)
                            {_:View,_:MotionEvent ->
                                Dragging(
                                        actions.doubleTapDragAction.opt,
                                        Xy(event.rawX,event.rawY),
                                        event.eventTime)
                            }.let {{_:DefaultTouchSessionFactory ->it}}
                        }
                        is Opt.None ->
                        {
                            val tapAction = actions.tapAction.opt
                            if (tapAction != null)
                            {
                                tapAction.doAction(v)
                                feedbackListener.onClicked(v)
                            }
                            context.touchSessionFactory
                        }
                    }
                }
            }
        }

        class AwaitUpOrDrag(
                val upAction:TouchHandler.OneShotAction,
                val dragAction:TouchHandler.DragAction,
                viewConfiguration:ViewConfiguration,
                val downPosition:Xy)
            :TouchSession()
        {
            private val squaredTouchSlop = viewConfiguration.scaledTouchSlop.let {it*it}

            override fun touchContinue(v:View,event:MotionEvent):PrimaryPointerFilterTouchListener.Listener.TouchSession
            {
                val currentPosition = Xy(event.rawX,event.rawY)
                return if (currentPosition.minus(downPosition).squaredDistance >= squaredTouchSlop)
                {
                    Dragging(dragAction,currentPosition,event.eventTime)
                }
                else
                {
                    this
                }
            }

            override fun touchEnd(v:View,event:MotionEvent)
            {
                upAction.doAction(v)
            }
        }

        class AwaitUp(
                val upAction:TouchHandler.OneShotAction)
            :TouchSession()
        {
            override fun touchContinue(v:View,event:MotionEvent) = this
            override fun touchEnd(v:View,event:MotionEvent) = upAction.doAction(v)
        }

        class Dragging(
                dragAction:TouchHandler.DragAction,
                firstPosition:Xy,
                dragStartEventTime:Long)
            :TouchSession()
        {
            private var lastPosition = firstPosition
            private val deltaMoveSamples = MovingWindowBuffer<Xy>(5)
            private val dragContinuation = dragAction.dragStart(firstPosition,dragStartEventTime)

            override fun touchContinue(v:View,event:MotionEvent):TouchSession
            {
                val currentPosition = Xy(event.rawX,event.rawY)
                val deltaPosition = currentPosition-lastPosition
                lastPosition = currentPosition
                dragContinuation.dragContinue(currentPosition,deltaPosition,event.eventTime)
                deltaMoveSamples.add(deltaPosition)
                return this
            }

            override fun touchEnd(v:View,event:MotionEvent)
            {
                val flingVelocity = deltaMoveSamples.consume().let()
                {samples ->
                    val denominator = samples.size.coerceAtLeast(1)
                    samples
                            .fold(Xy(0f,0f)) {acc,next -> acc+next}
                            .let {Xy(it.x/denominator,it.y/denominator)}
                }
                dragContinuation.dragEnd(flingVelocity,event.eventTime)
            }
        }
    }
}

private class MovingWindowBuffer<Element:Any>(maxSampleCount:Int)
{
    private val samples = ArrayBlockingQueue<Element>(maxSampleCount)
    private val lock = ReentrantLock()

    fun add(sample:Element) = lock.withLock()
    {
        if (samples.remainingCapacity() == 0)
        {
            samples.take()
        }
        samples.put(sample)
    }

    fun consume():List<Element> = lock.withLock()
    {
        return generateSequence {samples.poll()}.toList()
    }
}

private fun TouchHandler.OneShotAction.alsoDo(block:(View)->Unit):TouchHandler.OneShotAction
{
    val original = this
    return object:TouchHandler.OneShotAction
    {
        override fun doAction(v: View) {
            original.doAction(v)
            block(v)
        }
    }
}

private fun TouchHandler.DragAction.alsoDo(block:()->Unit):TouchHandler.DragAction
{
    val original = this
    return object:TouchHandler.DragAction
    {
        override fun dragStart(firstPosition:Xy,eventTime:Long):TouchHandler.DragAction.DragContinuation
        {
            val continuation = original.dragStart(firstPosition,eventTime)
            return object:TouchHandler.DragAction.DragContinuation by continuation
            {
                override fun dragEnd(flingVelocity:Xy,eventTime:Long)
                {
                    block()
                    continuation.dragEnd(flingVelocity,eventTime)
                }
            }
        }
    }
}

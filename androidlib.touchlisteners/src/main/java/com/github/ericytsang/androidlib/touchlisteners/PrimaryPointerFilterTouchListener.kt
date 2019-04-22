package com.github.ericytsang.androidlib.touchlisteners

import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.multidex.BuildConfig
import com.github.ericytsang.androidlib.core.DoLog
import com.github.ericytsang.androidlib.core.info
import com.github.ericytsang.androidlib.core.postOnUiThread
import kotlin.random.Random

class PrimaryPointerFilterTouchListener(
        _listener:Listener)
    :AbstractTouchListener.Listener,DoLog
{
    private var idAndTouchSession:IdAndTouchSession? = null
        set(value)
        {
            field?.latestMotionEvent?.recycle()
            field = value?.copy(latestMotionEvent = MotionEvent.obtain(value.latestMotionEvent))
        }
    var listener = _listener

    override fun touchBegin(v:View,event:MotionEvent)
    {
        if (idAndTouchSession == null)
        {
            val ogIdAndTouchSession = IdAndTouchSession(
                    primaryPointerId = event.getPointerId(event.actionIndex),
                    touchSession = listener.touchBegin(v,event),
                    latestMotionEvent = event)
            idAndTouchSession = ogIdAndTouchSession

            // artificially give ourselves a touchContinue event if we haven't
            // touch ended yet because some phones don't constantly stream
            // touchContinue events if the pointer is not moving...and when that
            // is the case, we fail to detect long click events....
            postOnUiThread(ViewConfiguration.getLongPressTimeout().toLong())
            {
                val idAndTouchSession = idAndTouchSession?:return@postOnUiThread
                if (idAndTouchSession.touchStartId == ogIdAndTouchSession.touchStartId)
                {
                    info(BuildConfig.DEBUG) {"send artificial touch event"}
                    val eventWithModifiedEventTime = MotionEvent.obtain(
                            idAndTouchSession.latestMotionEvent.downTime,
                            idAndTouchSession.latestMotionEvent.downTime+ViewConfiguration.getLongPressTimeout()+1,
                            MotionEvent.ACTION_MOVE,
                            idAndTouchSession.latestMotionEvent.rawX,
                            idAndTouchSession.latestMotionEvent.rawY,
                            idAndTouchSession.latestMotionEvent.metaState)
                    touchContinue(v,eventWithModifiedEventTime)
                    eventWithModifiedEventTime.recycle()
                }
            }
        }
    }

    override fun touchContinue(v:View,event:MotionEvent)
    {
        val idAndTouchSession = idAndTouchSession
        if (idAndTouchSession?.primaryPointerId == event.getPointerId(event.actionIndex))
        {
            this.idAndTouchSession = idAndTouchSession.copy(
                    touchSession = idAndTouchSession.touchSession.touchContinue(v,event),
                    latestMotionEvent = event)
        }
    }

    override fun touchEnd(v:View,event:MotionEvent)
    {
        val idAndTouchSession = idAndTouchSession
        if (idAndTouchSession?.primaryPointerId == event.getPointerId(event.actionIndex))
        {
            idAndTouchSession.touchSession.touchEnd(v,event)
            this.idAndTouchSession = null
        }
    }

    private data class IdAndTouchSession(
            val touchStartId:Long = Random.nextLong(),
            val primaryPointerId:Int,
            val touchSession:Listener.TouchSession,
            val latestMotionEvent:MotionEvent)

    interface Listener
    {
        fun touchBegin(v:View,event:MotionEvent):TouchSession
        interface TouchSession
        {
            fun touchContinue(v:View,event:MotionEvent):TouchSession
            fun touchEnd(v:View,event:MotionEvent)
        }
    }
}

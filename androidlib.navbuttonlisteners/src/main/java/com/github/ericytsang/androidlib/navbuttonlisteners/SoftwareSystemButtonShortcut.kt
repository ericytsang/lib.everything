package com.github.ericytsang.androidlib.navbuttonlisteners

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class SoftwareSystemButtonShortcut(
        val viewIdResourceName:Set<String>,
        val action:(AccessibilityEvent)->Unit)
{
    companion object
    {
        val RECENT_APPS = "com.android.systemui:id/recent_apps"
        val HOME = "com.android.systemui:id/home_button"
        val BACK = "com.android.systemui:id/back"
    }

    fun onAccessibilityEvent(event:AccessibilityEvent)
    {
        // ignore non-long-click event types
        if (event.eventType != AccessibilityEvent.TYPE_VIEW_LONG_CLICKED)
        {
            return
        }

        // toggle split screen if long click on overview button detected
        val eventViewIdResourceName = try
        {
            val source:AccessibilityNodeInfo? = event.source
            source?.refresh()
            source?.viewIdResourceName
        }
        catch(e:Throwable)
        {
            throw BenignException(e)
        }
        if (eventViewIdResourceName in viewIdResourceName)
        {
            action(event)
        }
    }
}

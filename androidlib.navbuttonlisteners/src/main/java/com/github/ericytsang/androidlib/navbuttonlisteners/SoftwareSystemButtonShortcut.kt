package com.github.ericytsang.androidlib.navbuttonlisteners

import android.content.Context
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.github.ericytsang.multiwindow.app.android.logExceptionSilently

sealed class SoftwareSystemButtonShortcut(
        val context:Context,
        val viewIdResourceName:String,
        val source:AppService.Params.ToggleSplitScreenMode.Source)
    :AppService.LifecycleStrategy
{
    companion object
    {
        private val SYSTEM_UI_PACKAGE = "com.android.systemui"
        private val RECENT_APPS = ":id/recent_apps"
        private val HOME_BUTTON = ":id/home_button"
        private val BACK = ":id/back"
    }

    override fun close() = Unit

    override fun onAccessibilityEvent(event:AccessibilityEvent)
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
            logExceptionSilently(e)
            null
        }
        if ({true}()
                && viewIdResourceName == eventViewIdResourceName)
        {
            AppService.start(context,AppService.Params.ToggleSplitScreenMode(source))
        }
    }

    class OverviewButtonShortcut(context:Context)
        :SoftwareSystemButtonShortcut(context,SYSTEM_UI_PACKAGE+RECENT_APPS,AppService.Params.ToggleSplitScreenMode.Source.Navigation.Overview(AppService.Params.ToggleSplitScreenMode.Source.Navigation.Ware.Soft()))

    class HomeButtonShortcut(context:Context)
        :SoftwareSystemButtonShortcut(context,SYSTEM_UI_PACKAGE+HOME_BUTTON,AppService.Params.ToggleSplitScreenMode.Source.Navigation.Home(AppService.Params.ToggleSplitScreenMode.Source.Navigation.Ware.Soft()))

    class BackButtonShortcut(context:Context)
        :SoftwareSystemButtonShortcut(context,SYSTEM_UI_PACKAGE+BACK,AppService.Params.ToggleSplitScreenMode.Source.Navigation.Back(AppService.Params.ToggleSplitScreenMode.Source.Navigation.Ware.Soft()))

    class CustomButtonShortcut(
            context:Context,
            viewIdResourceName:String)
        :SoftwareSystemButtonShortcut(
            context,
            viewIdResourceName,
            AppService.Params.ToggleSplitScreenMode.Source.Navigation.Custom(
                    viewIdResourceName,
                    AppService.Params.ToggleSplitScreenMode.Source.Navigation.Ware.Soft()))
}

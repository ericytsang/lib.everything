package com.github.ericytsang.androidlib.core.context

import android.app.Activity
import android.app.Application
import android.app.Service
import android.content.Context
import androidx.fragment.app.Fragment

sealed class WrappedContext
{
    abstract val context:Context

    sealed class BackgroundContext:WrappedContext()
    {
        abstract override val context:Context

        data class ApplicationCtx(override val context:Application):BackgroundContext()

        sealed class ForegroundContext:BackgroundContext()
        {
            data class ServiceCtx(override val context:Service):ForegroundContext()
            data class ActivityCtx(override val context:Activity):ForegroundContext()
            data class FragmentCtx(val fragment:Fragment):ForegroundContext()
            {
                override val context:Context get() = fragment.activity!!
            }
            sealed class ForResultCtx:ForegroundContext()
            {
                abstract val requestCode:Int
                data class ActivityForResultCtx(
                        val activityContext:ActivityCtx,
                        override val requestCode:Int)
                    :ForResultCtx()
                {
                    override val context:Context get() = activityContext.context
                }
                data class FragmentForResultCtx(
                        val fragmentContext:FragmentCtx,
                        override val requestCode:Int)
                    :ForResultCtx()
                {
                    override val context:Context get() = fragmentContext.context
                }
            }
        }
    }
}

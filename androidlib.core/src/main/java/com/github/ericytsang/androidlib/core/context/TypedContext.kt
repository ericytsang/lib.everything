package com.github.ericytsang.androidlib.core.context

import android.app.Activity
import android.app.Application
import android.app.Service
import android.content.Context
import androidx.fragment.app.Fragment

sealed class TypedContext
{
    abstract val context:Context

    sealed class BackgroundContext:TypedContext()
    {
        abstract override val context:Context

        data class ApplicationContext(override val context:Application):BackgroundContext()

        sealed class ForegroundContext:BackgroundContext()
        {
            data class ServiceContext(override val context:Service):ForegroundContext()
            data class ActivityContext(override val context:Activity):ForegroundContext()
            data class FragmentContext(val fragment:Fragment):ForegroundContext()
            {
                override val context:Context get() = fragment.activity!!
            }
            sealed class ForResultContext:ForegroundContext()
            {
                abstract val requestCode:Int
                data class ActivityForResultContext(
                        val activityContext:ActivityContext,
                        override val requestCode:Int)
                    :ForResultContext()
                {
                    override val context:Context get() = activityContext.context
                }
                data class FragmentForResultContext(
                        val fragmentContext:FragmentContext,
                        override val requestCode:Int)
                    :ForResultContext()
                {
                    override val context:Context get() = fragmentContext.context
                }
            }
        }
    }
}

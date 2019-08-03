package com.github.ericytsang.androidlib.core.intent

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import com.github.ericytsang.androidlib.core.context.WrappedContext.BackgroundContext
import com.github.ericytsang.androidlib.core.context.WrappedContext
import com.github.ericytsang.androidlib.core.context.WrappedContext.BackgroundContext.ForegroundContext
import com.github.ericytsang.androidlib.core.context.WrappedContext.BackgroundContext.ForegroundContext.ForResultCtx
import java.io.Serializable

sealed class StartableIntent<TContext:WrappedContext>:Serializable
{
    abstract fun start(context:TContext)
    abstract fun toPendingIntent(context:Context,requestCode:Int,flags:Int):PendingIntent

    sealed class StartableForResultIntent:StartableIntent<ForResultCtx>()
    {
        class ActivityForResultIntent
        internal constructor(
                private val intent:(Context)->Intent)
            :StartableForResultIntent()
        {
            companion object:StartableIntentFactory<ActivityForResultIntent>
            {
                override fun make(intentFactory:(Context)->Intent) = ActivityForResultIntent(intentFactory)
            }

            override fun start(context:ForResultCtx) = when(context)
            {
                is ForResultCtx.ActivityForResultCtx -> context.activityContext.context.startActivityForResult(intent(context.context),context.requestCode)
                is ForResultCtx.FragmentForResultCtx -> context.fragmentContext.fragment.startActivityForResult(intent(context.context),context.requestCode)
            }

            override fun toPendingIntent(context:Context,requestCode:Int,flags:Int):PendingIntent
            {
                return PendingIntent.getActivity(context,requestCode,intent(context),flags)
            }
        }
    }

    sealed class StartableForegroundIntent:StartableIntent<ForegroundContext>()
    {
        class ActivityIntent
        internal constructor(
                private val intent:(Context)->Intent)
            :StartableForegroundIntent()
        {
            companion object:StartableIntentFactory<ActivityIntent>
            {
                override fun make(intentFactory:(Context)->Intent) = ActivityIntent(intentFactory)
            }

            override fun start(context:ForegroundContext)
            {
                context.context.startActivity(intent(context.context))
            }

            override fun toPendingIntent(context:Context,requestCode:Int,flags:Int):PendingIntent
            {
                return PendingIntent.getActivity(context,requestCode,intent(context),flags)
            }
        }

        class BroadcastIntent
        internal constructor(
                private val intent:(Context)->Intent)
            :StartableForegroundIntent()
        {
            companion object:StartableIntentFactory<BroadcastIntent>
            {
                override fun make(intentFactory:(Context)->Intent) = BroadcastIntent(intentFactory)
            }

            override fun start(context:ForegroundContext)
            {
                context.context.sendBroadcast(intent(context.context))
            }

            override fun toPendingIntent(context:Context,requestCode:Int,flags:Int):PendingIntent
            {
                return PendingIntent.getBroadcast(context,requestCode,intent(context),flags)
            }
        }

        class ServiceIntent
        internal constructor(
                private val intent:(Context)->Intent)
            :StartableForegroundIntent()
        {
            companion object:StartableIntentFactory<ServiceIntent>
            {
                override fun make(intentFactory:(Context)->Intent) = ServiceIntent(intentFactory)
            }

            override fun start(context:ForegroundContext)
            {
                context.context.startService(intent(context.context))
            }

            override fun toPendingIntent(context:Context,requestCode:Int,flags:Int):PendingIntent
            {
                return PendingIntent.getService(context,requestCode,intent(context),flags)
            }
        }
    }

    sealed class StartableBackgroundIntent:StartableIntent<BackgroundContext>()
    {
        class ForegroundServiceIntent
        internal constructor(
                private val intent:(Context)->Intent)
            :StartableBackgroundIntent()
        {
            companion object:StartableIntentFactory<ForegroundServiceIntent>
            {
                override fun make(intentFactory:(Context)->Intent) = ForegroundServiceIntent(intentFactory)
            }

            override fun start(context:BackgroundContext)
            {
                ContextCompat.startForegroundService(context.context,intent(context.context))
            }

            override fun toPendingIntent(context:Context,requestCode:Int,flags:Int):PendingIntent
            {
                return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                {
                    PendingIntent.getForegroundService(context,requestCode,intent(context),flags)
                }
                else
                {
                    PendingIntent.getService(context,requestCode,intent(context),flags)
                }
            }
        }
    }

    interface StartableIntentFactory<TStartableIntent:StartableIntent<*>>
    {
        fun make(intentFactory:(Context)->Intent):TStartableIntent
    }
}

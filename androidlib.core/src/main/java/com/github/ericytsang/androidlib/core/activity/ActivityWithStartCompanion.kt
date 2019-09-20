package com.github.ericytsang.androidlib.core.activity

import android.content.Context
import com.github.ericytsang.androidlib.core.context.TypedContext
import com.github.ericytsang.androidlib.core.intent.StartableIntent
import java.io.Serializable

abstract class ActivityWithStartCompanion<Subclass:Context,IParams:Serializable>
    :ContextCompanion<Subclass,TypedContext.BackgroundContext.ForegroundContext,IParams,StartableIntent.StartableForegroundIntent.ActivityIntent>(StartableIntent.StartableForegroundIntent.ActivityIntent)
{
    fun start(context:TypedContext.BackgroundContext.ForegroundContext,params:IParams,extraFlags:Int = 0)
    {
        toIntent(params,extraFlags).start(context)
    }
}

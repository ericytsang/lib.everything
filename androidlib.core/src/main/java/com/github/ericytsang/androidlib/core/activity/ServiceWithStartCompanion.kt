package com.github.ericytsang.androidlib.core.activity

import android.content.Context
import com.github.ericytsang.androidlib.core.context.TypedContext
import com.github.ericytsang.androidlib.core.intent.StartableIntent
import java.io.Serializable

abstract class ServiceWithStartCompanion<Subclass:Context,IContext:TypedContext,IParams:Serializable,OIntent:StartableIntent<IContext>>(
        startableIntentFactory:StartableIntent.StartableIntentFactory<OIntent>)
    :ContextCompanion<Subclass,TypedContext.BackgroundContext.ForegroundContext,IParams,StartableIntent.StartableForegroundIntent.ServiceIntent>(StartableIntent.StartableForegroundIntent.ServiceIntent)
{
    fun start(context:TypedContext.BackgroundContext.ForegroundContext,params:IParams,extraFlags:Int = 0)
    {
        toIntent(params,extraFlags).start(context)
    }
}

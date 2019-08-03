package com.github.ericytsang.androidlib.core.activity

import android.content.Context
import com.github.ericytsang.androidlib.core.context.WrappedContext
import com.github.ericytsang.androidlib.core.intent.StartableIntent
import com.github.ericytsang.androidlib.core.intent.StartableIntent.StartableIntentFactory
import java.io.Serializable

abstract class ContextCompanionWithStart<Subclass:Context,StartContext:WrappedContext,StartParams:Serializable,OIntent:StartableIntent<StartContext>>(
        startableIntentFactory:StartableIntentFactory<OIntent>)
    :ContextCompanion<Subclass,StartParams,OIntent>(startableIntentFactory)
{
    fun start(context:StartContext,params:StartParams,extraFlags:Int = 0)
    {
        toIntent(params,extraFlags).start(context)
    }
}

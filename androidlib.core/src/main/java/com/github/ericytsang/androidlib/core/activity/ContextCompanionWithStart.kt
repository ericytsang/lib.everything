package com.github.ericytsang.androidlib.core.activity

import android.content.Context
import com.github.ericytsang.androidlib.core.context.TypedContext
import com.github.ericytsang.androidlib.core.intent.StartableIntent
import com.github.ericytsang.androidlib.core.intent.StartableIntent.StartableIntentFactory
import java.io.Serializable

abstract class ContextCompanionWithStart<Subclass:Context,IContext:TypedContext,IParams:Serializable,OIntent:StartableIntent<IContext>>(
        startableIntentFactory:StartableIntentFactory<OIntent>)
    :ContextCompanion<Subclass,IContext,IParams,OIntent>(startableIntentFactory)
{
    fun start(context:IContext,params:IParams,extraFlags:Int = 0)
    {
        toIntent(params,extraFlags).start(context)
    }
}

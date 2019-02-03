package com.github.ericytsang.androidlib.core.activity

import android.content.Context
import android.content.Intent
import java.io.Serializable

abstract class ContextCompanionWithStart<Contextt:Context,Params:Serializable>(
        startableIntentFactory:((Context)->Intent)->StartableIntent)
    :ContextCompanion<Contextt,Params>(startableIntentFactory)
{
    fun start(context:Context,params:Params,extraFlags:Int = 0)
    {
        toIntent(params,extraFlags).start(context)
    }
}

package com.github.ericytsang.androidlib.core.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import java.io.Serializable

abstract class ContextCompanionWithStartForResult<Contextt:Context,Params:Serializable>(
        startableIntentFactory:((Context)->Intent)->StartableIntent)
    :ContextCompanion<Contextt,Params>(startableIntentFactory)
{
    fun startForResult(context:Activity,params:Params,requestCode:Int,extraFlags:Int = 0)
    {
        context.startActivityForResult(
                toIntent(params,extraFlags).intent(context),
                requestCode)
    }
    fun startForResult(context:Fragment,params:Params,requestCode:Int,extraFlags:Int = 0)
    {
        context.startActivityForResult(
                toIntent(params,extraFlags).intent(context.activity!!),
                requestCode)
    }
}

package com.github.ericytsang.lib.android.activity

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.io.Serializable

abstract class ContextCompanion<Contextt:Context,ContextParams:Serializable>(
        val startableIntentFactory:((Context)->Intent)->StartableIntent)
{
    private val activityParamsExtraKey = "${ContextCompanion::class.qualifiedName}.activityParamsExtraKey"
    protected abstract val contextClass:Class<Contextt>
    fun toIntent(params:ContextParams):StartableIntent
    {
        return startableIntentFactory()
        {
            context:Context->
            val intent = Intent(context,contextClass)
            intent.putExtra(activityParamsExtraKey,params)
            intent.addFlags(getFlagsForIntent(params).fold(0) {acc, i -> acc or i })
        }
    }
    protected open fun getFlagsForIntent(params:ContextParams):Set<Int> = setOf(0)
    protected fun fromIntent(startingIntent:Intent):ContextParams
    {
        return startingIntent.getSerializableExtra(
                activityParamsExtraKey) as ContextParams
    }
}

interface StartableIntent:Serializable
{
    val intent:(Context)->Intent
    fun start(context:Context)
    fun toPendingIntent(context:Context,requestCode:Int,flags:Int):PendingIntent
}

class BroadcastIntent(
        override val intent:(Context)->Intent)
    :StartableIntent
{
    companion object
    {
        val FACTORY:((Context)->Intent)->BroadcastIntent = fun(intentFactory:(Context)->Intent):BroadcastIntent
        {
            return BroadcastIntent(intentFactory)
        }
    }

    override fun start(context:Context)
    {
        context.sendBroadcast(intent(context))
    }

    override fun toPendingIntent(context:Context,requestCode:Int,flags:Int):PendingIntent
    {
        return PendingIntent.getBroadcast(context,requestCode,intent(context),flags)
    }
}

class ServiceIntent(
        override val intent:(Context)->Intent)
    :StartableIntent
{
    companion object
    {
        val FACTORY:((Context)->Intent)->ServiceIntent = fun(intentFactory:(Context)->Intent):ServiceIntent
        {
            return ServiceIntent(intentFactory)
        }
    }

    override fun start(context:Context)
    {
        context.startService(intent(context))
    }

    override fun toPendingIntent(context:Context,requestCode:Int,flags:Int):PendingIntent
    {
        return PendingIntent.getService(context,requestCode,intent(context),flags)
    }
}

class ActivityIntent(
        override val intent:(Context)->Intent)
    :StartableIntent
{
    companion object
    {
        val FACTORY:((Context)->Intent)->ActivityIntent = fun(intentFactory:(Context)->Intent):ActivityIntent
        {
            return ActivityIntent(intentFactory)
        }
    }

    override fun start(context:Context)
    {
        context.startActivity(intent(context))
    }

    override fun toPendingIntent(context:Context,requestCode:Int,flags:Int):PendingIntent
    {
        return PendingIntent.getActivity(context,requestCode,intent(context),flags)
    }
}

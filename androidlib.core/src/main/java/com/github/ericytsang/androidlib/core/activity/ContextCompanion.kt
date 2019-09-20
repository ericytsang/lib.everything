package com.github.ericytsang.androidlib.core.activity

import android.content.Context
import android.content.Intent
import com.github.ericytsang.androidlib.core.intent.StartableIntent
import java.io.Serializable
import kotlin.reflect.KClass
import kotlin.reflect.full.cast

abstract class ContextCompanion<Subclass:Context,StartParams:Serializable,OIntent:StartableIntent<*>>(
        private val startableIntentFactory:StartableIntent.StartableIntentFactory<OIntent>)
{
    private val activityParamsExtraKey = "${ContextCompanion::class.qualifiedName}.activityParamsExtraKey"
    protected abstract val contextClass:KClass<Subclass>
    protected abstract val paramsClass:KClass<StartParams>
    fun toIntent(params:StartParams,extraFlags:Int = 0):OIntent
    {
        return startableIntentFactory.make()
        {
            context:Context->
            val intent = Intent(context,contextClass.java)
            intent.putExtra(activityParamsExtraKey,params)
            intent.addFlags(getFlagsForIntent(params)
                    .plus(extraFlags)
                    .fold(0) {acc, i -> acc or i })
        }
    }
    open fun getFlagsForIntent(params:StartParams):Set<Int> = setOf(0)
    fun fromIntent(startingIntent:Intent):StartParams
    {
        val serializable = startingIntent.getSerializableExtra(activityParamsExtraKey)
        return paramsClass.cast(serializable!!)
    }
}


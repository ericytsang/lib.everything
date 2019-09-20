package com.github.ericytsang.androidlib.core.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.github.ericytsang.androidlib.core.context.TypedContext.BackgroundContext
import com.github.ericytsang.androidlib.core.context.wrap
import com.github.ericytsang.androidlib.core.intent.StartableIntent.StartableForegroundIntent
import com.github.ericytsang.androidlib.core.intent.StartableIntent.StartableForegroundIntent.ActivityIntent
import com.github.ericytsang.androidlib.core.kClass
import java.io.Serializable

class IntentLauncherActivity:AppCompatActivity()
{
    companion object:ContextCompanionWithStart<IntentLauncherActivity,BackgroundContext.ForegroundContext,Params,ActivityIntent>(ActivityIntent)
    {
        override val contextClass get() = kClass<IntentLauncherActivity>()
        override val paramsClass get() = kClass<Params>()
        override fun getFlagsForIntent(params:Params):Set<Int>
        {
            return setOf(
                    Intent.FLAG_ACTIVITY_NO_HISTORY,
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        }
    }

    class Params
    constructor(
            val intents:List<StartableForegroundIntent>)
        :Serializable
    {
        companion object
        {
            fun of(vararg intents:StartableForegroundIntent):Params
            {
                return Params(intents.toList())
            }
        }
    }

    override fun onCreate(savedInstanceState:Bundle?)
    {
        // call super
        super.onCreate(savedInstanceState)

        // parse parameters, and start the intents
        val params = fromIntent(intent)
        params.intents.forEach {it.start(wrap())}

        // finish activity so it doesn't show up
        finish()
    }
}

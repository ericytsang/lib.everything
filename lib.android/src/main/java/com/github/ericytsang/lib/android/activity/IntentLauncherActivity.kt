package com.github.ericytsang.lib.android.activity

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import java.io.Serializable

class IntentLauncherActivity:AppCompatActivity()
{
    companion object:ContextCompanionWithStart<IntentLauncherActivity,Params>(ActivityIntent.FACTORY)
    {
        override val contextClass:Class<IntentLauncherActivity> get() = IntentLauncherActivity::class.java
        override fun getFlagsForIntent(params:Params):Set<Int>
        {
            return setOf(
                    Intent.FLAG_ACTIVITY_NO_HISTORY,
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        }
    }

    class Params
    constructor(
            val intents:List<StartableIntent>)
        :Serializable
    {
        companion object
        {
            fun of(vararg intents:StartableIntent):Params
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
        params.intents.forEach {it.start(this)}

        // finish activity so it doesn't show up
        finish()
    }
}
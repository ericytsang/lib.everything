package com.github.ericytsang.androidlib.alertdialog

import android.os.Bundle
import android.view.ViewGroup
import com.github.ericytsang.androidlib.alertdialog.databinding.ActivityAlertDialogBinding
import com.github.ericytsang.androidlib.core.activity.BaseActivity
import com.github.ericytsang.androidlib.core.activity.ContextCompanionWithStart
import com.github.ericytsang.androidlib.core.context.WrappedContext.BackgroundContext.ForegroundContext
import com.github.ericytsang.androidlib.core.fromHtml
import com.github.ericytsang.androidlib.core.intent.StartableIntent.StartableForegroundIntent.ActivityIntent
import java.io.Closeable
import java.io.Serializable

class AlertDialogActivity
    :BaseActivity<
        AlertDialogActivity.Created,
        BaseActivity.NoOpState<AlertDialogActivity>>()
{
    companion object:ContextCompanionWithStart<AlertDialogActivity,ForegroundContext,Params,ActivityIntent>(ActivityIntent)
    {
        override val contextClass:Class<AlertDialogActivity> get() = AlertDialogActivity::class.java
        override fun getFlagsForIntent(params:Params):Set<Int>
        {
            return params.additionalIntentFlags
        }
    }

    data class Params(
            val themeResId:Int,
            val title:String? = null,
            val bodyText:String,
            val buttonText:String? = null,
            val additionalIntentFlags:Set<Int> = emptySet(),
            val onButtonPress:(AlertDialogActivity)->Unit = {})
        :Serializable

    override fun onCreate(savedInstanceState:Bundle?)
    {
        // set activity theme
        setTheme(fromIntent(intent).themeResId)

        // do onCreate
        super.onCreate(savedInstanceState)
    }

    override fun makeCreated(methodOverload:MethodOverload,contentView:ViewGroup) = Created(this,fromIntent(intent),contentView)
    class Created(
            val activity:AlertDialogActivity,
            val params:Params,
            contentView:ViewGroup)
        :Closeable
    {
        init
        {
            // title
            if (params.title != null)
            {
                activity.title = params.title
            }

            val layout = ActivityAlertDialogBinding
                    .inflate(activity.layoutInflater,contentView,false)
                    .apply {activity.setContentView(root)}

            // textview
            layout.textview.text = fromHtml(params.bodyText)

            // button
            layout.buttonDismiss.text = params.buttonText?:activity.getText(android.R.string.ok)
            layout.buttonDismiss.setOnClickListener()
            {
                params.onButtonPress(activity)
                activity.finish()
            }
        }

        override fun close() = Unit
    }

    override fun makeResumed(methodOverload:MethodOverload,created:AlertDialogActivity.Created) = NoOpState(this)
}

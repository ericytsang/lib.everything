package com.github.ericytsang.lib.android.alertdialog.activity

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import com.github.ericytsang.lib.android.R
import com.github.ericytsang.lib.android.activity.ActivityIntent
import com.github.ericytsang.lib.android.activity.BaseActivity
import com.github.ericytsang.lib.android.activity.ContextCompanionWithStart
import com.github.ericytsang.lib.android.fromHtml
import com.github.ericytsang.lib.android.layoutInflater
import kotlinx.android.extensions.LayoutContainer
import java.io.Closeable
import java.io.Serializable
import kotlinx.android.synthetic.main.activity__alert_dialog.*

class AlertDialogActivity
    :BaseActivity<
        AlertDialogActivity.Created,
        BaseActivity.NoOpState<AlertDialogActivity>>()
{
    companion object:ContextCompanionWithStart<AlertDialogActivity,Params>(ActivityIntent.FACTORY)
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

            val layout = Layout(contentView).apply {activity.setContentView(containerView)}

            // textview
            layout.textview.text = fromHtml(params.bodyText)

            // button
            layout.button__dismiss.text = params.buttonText?:activity.getText(android.R.string.ok)
            layout.button__dismiss.setOnClickListener()
            {
                params.onButtonPress(activity)
                activity.finish()
            }
        }

        override fun close() = Unit
    }

    override fun makeResumed(methodOverload:MethodOverload,created:AlertDialogActivity.Created) = NoOpState(this)

    class Layout(root:ViewGroup):LayoutContainer
    {
        override val containerView:View = root.context.layoutInflater.inflate(R.layout.activity__alert_dialog,root,false)
    }
}

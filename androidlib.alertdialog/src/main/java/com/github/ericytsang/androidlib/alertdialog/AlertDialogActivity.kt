package com.github.ericytsang.androidlib.alertdialog

import android.os.Bundle
import android.view.ViewGroup
import com.github.ericytsang.androidlib.alertdialog.databinding.ActivityAlertDialogBinding
import com.github.ericytsang.androidlib.core.activity.ActivityWithResultCompanion
import com.github.ericytsang.androidlib.core.activity.BaseActivity
import com.github.ericytsang.androidlib.core.kClass
import com.github.ericytsang.androidlib.core.fromHtml
import java.io.Closeable
import java.io.Serializable

class AlertDialogActivity
    :BaseActivity<
        AlertDialogActivity.Created,
        BaseActivity.NoOpState<AlertDialogActivity>>()
{
    companion object:ActivityWithResultCompanion<AlertDialogActivity,Params,Result>()
    {
        override val contextClass get() = kClass<AlertDialogActivity>()
        override val paramsClass get() = kClass<Params>()
        override val resultClass get() = kClass<Result>()

        override fun getFlagsForIntent(params:Params):Set<Int>
        {
            return params.additionalIntentFlags
        }
    }

    data class Params(
            val themeResId:Int? = null,
            val title:String? = null,
            val bodyText:String,
            val buttonText:String? = null,
            val additionalIntentFlags:Set<Int> = emptySet())
        :Serializable

    enum class Result:Serializable
    {
        Ok,
        Cancelled,
        ;
    }

    override fun onCreate(savedInstanceState:Bundle?)
    {
        // set activity theme
        val themeResId = fromIntent(intent).themeResId
        if (themeResId != null)
        {
            setTheme(themeResId)
        }

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
                setOnActivityResult(activity,Result.Ok)
                activity.finish()
            }

            // default result
            setOnActivityResult(activity,Result.Cancelled)
        }

        override fun close() = Unit
    }

    override fun makeResumed(methodOverload:MethodOverload,created:AlertDialogActivity.Created) = NoOpState(this)
}

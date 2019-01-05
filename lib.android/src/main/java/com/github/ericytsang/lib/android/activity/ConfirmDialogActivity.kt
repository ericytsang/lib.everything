package com.github.ericytsang.lib.android.activity;

import android.view.ViewGroup
import com.github.ericytsang.lib.android.R
import com.github.ericytsang.lib.android.layoutInflater
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.activity__confirm_dialog.*
import java.io.Closeable
import java.io.Serializable

class ConfirmDialogActivity
    :BaseActivity<
        ConfirmDialogActivity.Created,
        BaseActivity.NoOpState<ConfirmDialogActivity>>()
{
    companion object:ActivityWithResultCompanion<ConfirmDialogActivity,Params,Result>()
    {
        override val contextClass get() = ConfirmDialogActivity::class.java
    }

    // Params

    data class Params(
            val title:String?,
            val prompt:String,
            val yesButton:ButtonConfig,
            val noButton:ButtonConfig)
        :Serializable

    data class ButtonConfig(
            val isEnabled:Boolean,
            val text:String,
            val color:Int?,
            val visibility:Int)
        :Serializable

    // Result

    sealed class Result:Serializable
    {
        data class ButtonPressed(
                val pressedButton:ButtonConfig)
            :
                Result(),
                Serializable
        class Cancelled
            :
                Result(),
                Serializable
    }

    // Created

    override fun makeCreated(methodOverload:MethodOverload,contentView:ViewGroup) = Created(this,fromIntent(intent))

    class Created(
            val activity:ConfirmDialogActivity,
            val params:Params)
        :Closeable
    {
        val layout = Layout(activity.findViewById(android.R.id.content))

        init
        {
            activity.setContentView(layout.containerView)
            if (params.title != null)
            {
                activity.title = params.title
            }
            setOnActivityResult(activity,Result.Cancelled())
            layout.textview.text = params.prompt
            layout.button__cancel.text = params.noButton.text
            layout.button__cancel.isEnabled = params.noButton.isEnabled
            layout.button__cancel.setTextColor(params.noButton.color ?: layout.button__cancel.currentTextColor)
            layout.button__cancel.visibility = params.noButton.visibility
            layout.button__cancel.setOnClickListener()
            {
                setOnActivityResult(activity,Result.ButtonPressed(params.noButton))
                activity.finish()
            }
            layout.button__ok.text = params.yesButton.text
            layout.button__ok.isEnabled = params.yesButton.isEnabled
            layout.button__ok.setTextColor(params.yesButton.color ?: layout.button__ok.currentTextColor)
            layout.button__ok.visibility = params.yesButton.visibility
            layout.button__ok.setOnClickListener()
            {
                setOnActivityResult(activity,Result.ButtonPressed(params.yesButton))
                activity.finish()
            }
        }

        override fun close() = Unit
    }

    // Resumed

    override fun makeResumed(methodOverload:MethodOverload,created:Created) = NoOpState(this)

    // Layout

    class Layout(val parent:ViewGroup):LayoutContainer
    {
        override val containerView = parent.context.layoutInflater.inflate(R.layout.activity__confirm_dialog,parent,false)
    }
}

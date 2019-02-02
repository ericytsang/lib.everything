package com.github.ericytsang.lib.android.confirmdialog.activity;

import android.view.ViewGroup
import com.github.ericytsang.lib.android.R
import com.github.ericytsang.lib.android.activity.ActivityWithResultCompanion
import com.github.ericytsang.lib.android.activity.BaseActivity
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
    companion object:ActivityWithResultCompanion<ConfirmDialogActivity,Params<*>,Result<*>>()
    {
        override val contextClass get() = ConfirmDialogActivity::class.java
    }

    enum class ButtonId:Serializable
    {
        YES_BUTTON,
        NO_BUTTON;
    }

    // Params

    data class Params<ExtraUserData:Serializable>(
            val title:String?,
            val prompt:String,
            val yesButton:ButtonConfig,
            val noButton:ButtonConfig,
            val extraUserData:ExtraUserData)
        :Serializable

    data class ButtonConfig(
            val isEnabled:Boolean,
            val text:String,
            val color:Int?,
            val visibility:Int)
        :Serializable

    // Result

    sealed class Result<ExtraUserData:Serializable>:Serializable
    {
        abstract val extraUserData:ExtraUserData
        data class ButtonPressed<ExtraUserData:Serializable>(
                val buttonId:ButtonId,
                override val extraUserData:ExtraUserData)
            :
                Result<ExtraUserData>(),
                Serializable
        data class Cancelled<ExtraUserData:Serializable>(
                override val extraUserData:ExtraUserData)
            :
                Result<ExtraUserData>(),
                Serializable
    }

    // Created

    override fun makeCreated(methodOverload:MethodOverload,contentView:ViewGroup) = Created(this,fromIntent(intent))

    class Created(
            val activity:ConfirmDialogActivity,
            val params:Params<*>)
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
            setOnActivityResult(activity,Result.Cancelled(params.extraUserData))
            layout.textview.text = params.prompt
            layout.button__cancel.text = params.noButton.text
            layout.button__cancel.isEnabled = params.noButton.isEnabled
            layout.button__cancel.setTextColor(params.noButton.color?:layout.button__cancel.currentTextColor)
            layout.button__cancel.visibility = params.noButton.visibility
            layout.button__cancel.setOnClickListener()
            {
                setOnActivityResult(activity,Result.ButtonPressed(ButtonId.NO_BUTTON,params.extraUserData))
                activity.finish()
            }
            layout.button__ok.text = params.yesButton.text
            layout.button__ok.isEnabled = params.yesButton.isEnabled
            layout.button__ok.setTextColor(params.yesButton.color?:layout.button__ok.currentTextColor)
            layout.button__ok.visibility = params.yesButton.visibility
            layout.button__ok.setOnClickListener()
            {
                setOnActivityResult(activity,Result.ButtonPressed(ButtonId.YES_BUTTON,params.extraUserData))
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

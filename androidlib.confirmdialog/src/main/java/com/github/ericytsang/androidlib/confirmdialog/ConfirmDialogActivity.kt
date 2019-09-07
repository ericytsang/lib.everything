package com.github.ericytsang.androidlib.confirmdialog

import android.view.ViewGroup
import com.github.ericytsang.androidlib.confirmdialog.databinding.ActivityConfirmDialogBinding
import com.github.ericytsang.androidlib.core.activity.ActivityWithResultCompanion
import com.github.ericytsang.androidlib.core.activity.BaseActivity
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
        val layout = ActivityConfirmDialogBinding
                .inflate(activity.layoutInflater,activity.findViewById(android.R.id.content),false)
                .apply {activity.setContentView(root)}

        init
        {
            if (params.title != null)
            {
                activity.title = params.title
            }
            setOnActivityResult(activity,Result.Cancelled(params.extraUserData))
            layout.textview.text = params.prompt
            layout.buttonCancel.text = params.noButton.text
            layout.buttonCancel.isEnabled = params.noButton.isEnabled
            layout.buttonCancel.setTextColor(params.noButton.color?:layout.buttonCancel.currentTextColor)
            layout.buttonCancel.visibility = params.noButton.visibility
            layout.buttonCancel.setOnClickListener()
            {
                setOnActivityResult(activity,Result.ButtonPressed(ButtonId.NO_BUTTON,params.extraUserData))
                activity.finish()
            }
            layout.buttonOk.text = params.yesButton.text
            layout.buttonOk.isEnabled = params.yesButton.isEnabled
            layout.buttonOk.setTextColor(params.yesButton.color?:layout.buttonOk.currentTextColor)
            layout.buttonOk.visibility = params.yesButton.visibility
            layout.buttonOk.setOnClickListener()
            {
                setOnActivityResult(activity,Result.ButtonPressed(ButtonId.YES_BUTTON,params.extraUserData))
                activity.finish()
            }
        }

        override fun close() = Unit
    }

    // Resumed

    override fun makeResumed(methodOverload:MethodOverload,created:Created) = NoOpState(this)
}

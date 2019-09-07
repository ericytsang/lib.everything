package com.github.ericytsang.androidlib.texinputdialog

import android.view.View
import android.view.ViewGroup
import com.github.ericytsang.androidlib.core.activity.ActivityWithResultCompanion
import com.github.ericytsang.androidlib.core.activity.BaseActivity
import com.github.ericytsang.androidlib.texinputdialog.databinding.ActivityTextInputDialogBinding
import java.io.Closeable
import java.io.Serializable

class TextInputDialogActivity
    :BaseActivity<
        TextInputDialogActivity.Created,
        BaseActivity.NoOpState<TextInputDialogActivity>>()
{
    companion object:ActivityWithResultCompanion<TextInputDialogActivity,StartParams,ResultParams>()
    {
        override val contextClass:Class<TextInputDialogActivity> get() = TextInputDialogActivity::class.java
    }

    data class StartParams(val title:String?,val prompt:String?,val text:String):Serializable

    data class ResultParams(val text:String,val cancelled:Boolean):Serializable

    override fun makeCreated(methodOverload:MethodOverload,contentView:ViewGroup) = Created(this,fromIntent(intent),contentView)
    class Created(
            val activity:TextInputDialogActivity,
            val startParams:StartParams,
            contentView:ViewGroup)
        :Closeable
    {
        val layout = ActivityTextInputDialogBinding
                .inflate(activity.layoutInflater,contentView,false)
                .apply {activity.setContentView(root)}
        init
        {

            // title
            if (startParams.title != null)
            {
                activity.title = startParams.title
            }

            // textview
            if (startParams.prompt == null)
            {
                layout.textview.visibility = View.GONE
            }
            else
            {
                layout.textview.text = startParams.prompt
            }

            // edittext
            layout.edittext.setText(startParams.text)
            layout.edittext.requestFocus()
            layout.edittext.selectAll()

            // activity result
            setOnActivityResult(activity,ResultParams(startParams.text,true))
            layout.buttonOk.setOnClickListener()
            {
                setOnActivityResult(activity,ResultParams(layout.edittext.text?.toString()?:"",false))
                activity.finish()
            }

            layout.buttonCancel.setOnClickListener()
            {
                activity.finish()
            }
        }

        override fun close() = Unit
    }

    override fun makeResumed(methodOverload:MethodOverload,created:Created) = NoOpState(this)
}

package com.github.ericytsang.lib.android.activity

import android.view.View
import android.view.ViewGroup
import com.github.ericytsang.lib.android.R
import com.github.ericytsang.lib.android.layoutInflater
import kotlinx.android.extensions.LayoutContainer
import java.io.Closeable
import java.io.Serializable
import kotlinx.android.synthetic.main.activity__text_input_dialog.*

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
        val layout = Layout(contentView).apply {activity.setContentView(containerView)}
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
            layout.button__ok.setOnClickListener()
            {
                setOnActivityResult(activity,ResultParams(layout.edittext.text?.toString()?:"",false))
                activity.finish()
            }

            layout.button__cancel.setOnClickListener()
            {
                activity.finish()
            }
        }

        override fun close() = Unit
    }

    override fun makeResumed(methodOverload:MethodOverload,created:Created) = NoOpState(this)

    class Layout(root:ViewGroup):LayoutContainer
    {
        override val containerView:View = root.context.layoutInflater.inflate(R.layout.activity__text_input_dialog,root,false)
    }
}

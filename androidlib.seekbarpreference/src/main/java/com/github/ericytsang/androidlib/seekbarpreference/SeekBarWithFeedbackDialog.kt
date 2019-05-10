package com.github.ericytsang.androidlib.seekbarpreference

import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.github.ericytsang.androidlib.core.activity.ActivityIntent
import com.github.ericytsang.androidlib.core.activity.ContextCompanionWithStart
import com.github.ericytsang.androidlib.core.layoutInflater
import com.github.ericytsang.androidlib.seekbar.SeekBarWithFeedback
import com.github.ericytsang.lib.optional.Opt
import com.github.ericytsang.lib.prop.RaiiProp
import com.github.ericytsang.lib.prop.value
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.activity__seek_bar_with_feedback_dialog.*
import java.io.Closeable
import java.io.Serializable

class SeekBarWithFeedbackDialog:AppCompatActivity()
{
    companion object:ContextCompanionWithStart<SeekBarWithFeedbackDialog,Params>(ActivityIntent.FACTORY)
    {
        override val contextClass:Class<SeekBarWithFeedbackDialog> get() = SeekBarWithFeedbackDialog::class.java
    }

    // params

    data class Params(
            val title:String,
            val oldValue:Int,
            val minValue:Int,
            val maxValue:Int,
            val labelTemplate:String,
            val onSetValue:(Context,Int)->Unit)
        :Serializable

    // created

    private val created = RaiiProp(Opt.none<Created>())

    override fun onCreate(savedInstanceState:Bundle?)
    {
        super.onCreate(savedInstanceState)
        created.value = {Opt.Some(Created(this,fromIntent(intent)))}
    }

    override fun onDestroy()
    {
        created.value = {Opt.None()}
        super.onDestroy()
    }

    private class Created(
            val activity:SeekBarWithFeedbackDialog,
            val params:Params)
        :Closeable
    {
        private var selectedColor = params.oldValue
        val layout = Layout(activity.findViewById(android.R.id.content))
        init
        {
            activity.setContentView(layout.containerView)
            activity.title = params.title
            layout.seekbar_with_feedback__dip.min.value = params.minValue
            layout.seekbar_with_feedback__dip.max.value = params.maxValue
            layout.seekbar_with_feedback__dip.progress.value = params.oldValue
            layout.seekbar_with_feedback__dip.labelTemplate.value = params.labelTemplate
            layout.seekbar_with_feedback__dip.listener = object:SeekBarWithFeedback.Listener
            {
                override fun onProgressChanged(source:SeekBarWithFeedback,progress:Int,fromUser:Boolean)
                {
                    params.onSetValue(activity,progress)
                }
            }
            layout.button__cancel.setOnClickListener()
            {
                activity.finish()
            }
            layout.button__ok.setOnClickListener()
            {
                selectedColor = layout.seekbar_with_feedback__dip.progress.value
                activity.finish()
            }
        }
        override fun close()
        {
            params.onSetValue(activity,selectedColor)
        }
    }

    // layout

    private class Layout(
            val parent:ViewGroup)
        :LayoutContainer
    {
        override val containerView = parent.context.layoutInflater.inflate(R.layout.activity__seek_bar_with_feedback_dialog,parent,false)
    }
}

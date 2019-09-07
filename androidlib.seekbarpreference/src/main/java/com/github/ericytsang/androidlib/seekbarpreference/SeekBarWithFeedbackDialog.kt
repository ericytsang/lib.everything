package com.github.ericytsang.androidlib.seekbarpreference

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.github.ericytsang.androidlib.core.activity.ContextCompanionWithStart
import com.github.ericytsang.androidlib.core.context.WrappedContext.BackgroundContext.ForegroundContext
import com.github.ericytsang.androidlib.core.intent.StartableIntent.StartableForegroundIntent.ActivityIntent
import com.github.ericytsang.androidlib.seekbar.SeekBarWithFeedback
import com.github.ericytsang.androidlib.seekbarpreference.databinding.ActivitySeekBarWithFeedbackDialogBinding
import com.github.ericytsang.lib.optional.Opt
import com.github.ericytsang.lib.prop.RaiiProp
import com.github.ericytsang.lib.prop.value
import java.io.Closeable
import java.io.Serializable

class SeekBarWithFeedbackDialog:AppCompatActivity()
{
    companion object:ContextCompanionWithStart<SeekBarWithFeedbackDialog,ForegroundContext,Params,ActivityIntent>(ActivityIntent)
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
        val layout = ActivitySeekBarWithFeedbackDialogBinding
                .inflate(activity.layoutInflater,activity.findViewById(android.R.id.content),false)
                .apply {activity.setContentView(root)}

        init
        {
            activity.title = params.title
            layout.seekbarWithFeedbackDip.min.value = params.minValue
            layout.seekbarWithFeedbackDip.max.value = params.maxValue
            layout.seekbarWithFeedbackDip.progress.value = params.oldValue
            layout.seekbarWithFeedbackDip.labelTemplate.value = params.labelTemplate
            layout.seekbarWithFeedbackDip.listener = object:SeekBarWithFeedback.Listener
            {
                override fun onProgressChanged(source:SeekBarWithFeedback,progress:Int,fromUser:Boolean)
                {
                    params.onSetValue(activity,progress)
                }
            }
            layout.buttonCancel.setOnClickListener()
            {
                activity.finish()
            }
            layout.buttonOk.setOnClickListener()
            {
                selectedColor = layout.seekbarWithFeedbackDip.progress.value
                activity.finish()
            }
        }

        override fun close()
        {
            params.onSetValue(activity,selectedColor)
        }
    }
}

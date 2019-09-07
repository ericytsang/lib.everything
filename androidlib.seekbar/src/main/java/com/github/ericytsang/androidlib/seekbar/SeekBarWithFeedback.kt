package com.github.ericytsang.androidlib.seekbar

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import android.widget.SeekBar
import com.github.ericytsang.androidlib.core.layoutInflater
import com.github.ericytsang.androidlib.seekbar.databinding.ViewSeekBarWithFeedbackBinding
import com.github.ericytsang.lib.prop.Prop
import com.github.ericytsang.lib.prop.listen
import com.github.ericytsang.lib.prop.value

class SeekBarWithFeedback(
        context:Context,
        attrs:AttributeSet)
    :LinearLayout(context,attrs)
{
    private val layout = ViewSeekBarWithFeedbackBinding.inflate(context.layoutInflater,this,true)

    var listener:Listener? = null

    val progress = object:Prop<Unit,Int>()
    {
        override fun doGet(context:Unit):Int
        {
            return (layout.SeekBarWithFeedbackSeekbar.progress+min.value)*valueCoefficient.value
        }
        override fun doSet(context:Unit,value:Int)
        { layout.SeekBarWithFeedbackSeekbar.progress = (value/valueCoefficient.value)-min.value }
    }

    val valueCoefficient = object:Prop<Unit,Int>()
    {
        private var field = 1
        override fun doGet(context:Unit):Int = field
        override fun doSet(context:Unit,value:Int) {field = value}
    }

    val labelTemplate = object:Prop<Unit,String>()
    {
        private var field = "{}"
        override fun doGet(context:Unit):String = field
        override fun doSet(context:Unit,value:String) {field = value}
    }

    val min = object:Prop<Unit,Int>()
    {
        private var internalValue:Int = 0
        override fun doGet(context:Unit):Int = internalValue
        override fun doSet(context:Unit,value:Int) {internalValue = value}
    }

    val max = object:Prop<Unit,Int>()
    {
        private var internalValue:Int = 0
        override fun doGet(context:Unit):Int = internalValue
        override fun doSet(context:Unit,value:Int) {internalValue = value}
    }

    init
    {
        // styling view from xml attributes
        val styledAttributes = context.obtainStyledAttributes(attrs,R.styleable.SeekBarWithFeedback)
        valueCoefficient.value = styledAttributes.getInt(R.styleable.SeekBarWithFeedback_valueCoefficient,1)
        labelTemplate.value = styledAttributes.getString(R.styleable.SeekBarWithFeedback_labelTemplate)?:"{}"
        min.value = styledAttributes.getInt(R.styleable.SeekBarWithFeedback_minimum,0)
        max.value = styledAttributes.getInt(R.styleable.SeekBarWithFeedback_maximum,100)
        styledAttributes.recycle()
    }

    init
    {
        // updating min and max on the progress bar
        listOf(min,max).listen()
        {
            layout.SeekBarWithFeedbackSeekbar.max = max.value-min.value
        }

        // updating text labels when needed
        listOf(progress,valueCoefficient,labelTemplate,min,max).listen()
        {
            layout.SeekBarWithFeedbackLabelMin.text = createLabelText(labelTemplate.value,min.value*valueCoefficient.value)
            layout.SeekBarWithFeedbackLabelMax.text = createLabelText(labelTemplate.value,max.value*valueCoefficient.value)
            layout.SeekBarWithFeedbackLabelValue.text = createLabelText(labelTemplate.value,progress.value)
        }

        // adding listener to seekbar
        val seekbarListener = object:SeekBar.OnSeekBarChangeListener
        {
            override fun onProgressChanged(seekBar:SeekBar?,progress:Int,fromUser:Boolean)
            {
                layout.SeekBarWithFeedbackLabelValue.text = createLabelText(labelTemplate.value,this@SeekBarWithFeedback.progress.value)
                listener?.onProgressChanged(this@SeekBarWithFeedback,this@SeekBarWithFeedback.progress.value,fromUser)
            }
            override fun onStartTrackingTouch(seekBar:SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar:SeekBar?) = Unit
        }.apply {onProgressChanged(null,0,false)}
        layout.SeekBarWithFeedbackSeekbar.setOnSeekBarChangeListener(seekbarListener)

        // adding listeners to buttons
        layout.SeekBarWithFeedbackButtonDecrement.setOnClickListener()
        {
            layout.SeekBarWithFeedbackSeekbar.progress--
            listener?.onProgressChanged(this@SeekBarWithFeedback,progress.value,true)
        }
        layout.SeekBarWithFeedbackButtonIncrement.setOnClickListener()
        {
            layout.SeekBarWithFeedbackSeekbar.progress++
            listener?.onProgressChanged(this@SeekBarWithFeedback,progress.value,true)
        }
    }

    interface Listener
    {
        fun onProgressChanged(source:SeekBarWithFeedback,progress:Int,fromUser:Boolean)
    }

    override fun setEnabled(enabled:Boolean)
    {
        layout.SeekBarWithFeedbackLabelMin.isEnabled = enabled
        layout.SeekBarWithFeedbackLabelMax.isEnabled = enabled
        layout.SeekBarWithFeedbackLabelValue.isEnabled = enabled
        layout.SeekBarWithFeedbackSeekbar.isEnabled = enabled
        super.setEnabled(enabled)
    }

    private fun createLabelText(labelTemplateStringResId:String,value:Int):String
    {
        return labelTemplateStringResId.replace("{}",value.toString())
    }
}

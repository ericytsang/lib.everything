package com.github.ericytsang.androidlib.seekbar

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.SeekBar
import com.github.ericytsang.androidlib.core.layoutInflater
import com.github.ericytsang.lib.prop.Prop
import com.github.ericytsang.lib.prop.listen
import com.github.ericytsang.lib.prop.value
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view__seek_bar_with_feedback.*

// todo: separate android library
class SeekBarWithFeedback(
        context:Context,
        attrs:AttributeSet)
    :LinearLayout(context,attrs)
{
    private val layout = Layout(this)

    var listener:Listener? = null

    val progress = object:Prop<Unit,Int>()
    {
        override fun doGet(context:Unit):Int
        { return layout.SeekBarWithFeedback__seekbar.progress*valueCoefficient.value }
        override fun doSet(context:Unit,value:Int)
        { layout.SeekBarWithFeedback__seekbar.progress = value/valueCoefficient.value }
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
        override fun doGet(context:Unit):Int = layout.SeekBarWithFeedback__seekbar.min
        override fun doSet(context:Unit,value:Int) {layout.SeekBarWithFeedback__seekbar.min = value}
    }

    val max = object:Prop<Unit,Int>()
    {
        override fun doGet(context:Unit):Int = layout.SeekBarWithFeedback__seekbar.max
        override fun doSet(context:Unit,value:Int) {layout.SeekBarWithFeedback__seekbar.max = value}
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
        // updating text labels when needed
        listOf(progress,valueCoefficient,labelTemplate,min,max).listen()
        {
            layout.SeekBarWithFeedback__label__min.text = createLabelText(labelTemplate.value,min.value*valueCoefficient.value)
            layout.SeekBarWithFeedback__label__max.text = createLabelText(labelTemplate.value,max.value*valueCoefficient.value)
            layout.SeekBarWithFeedback__label__value.text = createLabelText(labelTemplate.value,progress.value)
        }

        // adding listener to seekbar
        val seekbarListener = object:SeekBar.OnSeekBarChangeListener
        {
            override fun onProgressChanged(seekBar:SeekBar?,progress:Int,fromUser:Boolean)
            {
                layout.SeekBarWithFeedback__label__value.text = createLabelText(labelTemplate.value,progress*valueCoefficient.value)
                listener?.onProgressChanged(this@SeekBarWithFeedback,this@SeekBarWithFeedback.progress.value,fromUser)
            }
            override fun onStartTrackingTouch(seekBar:SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar:SeekBar?) = Unit
        }.apply {onProgressChanged(null,0,false)}
        layout.SeekBarWithFeedback__seekbar.setOnSeekBarChangeListener(seekbarListener)

        // adding listeners to buttons
        layout.SeekBarWithFeedback__button__decrement.setOnClickListener()
        {
            layout.SeekBarWithFeedback__seekbar.progress--
            listener?.onProgressChanged(this@SeekBarWithFeedback,progress.value,true)
        }
        layout.SeekBarWithFeedback__button__increment.setOnClickListener()
        {
            layout.SeekBarWithFeedback__seekbar.progress++
            listener?.onProgressChanged(this@SeekBarWithFeedback,progress.value,true)
        }
    }

    interface Listener
    {
        fun onProgressChanged(source:SeekBarWithFeedback,progress:Int,fromUser:Boolean)
    }

    override fun setEnabled(enabled:Boolean)
    {
        layout.SeekBarWithFeedback__label__min.isEnabled = enabled
        layout.SeekBarWithFeedback__label__max.isEnabled = enabled
        layout.SeekBarWithFeedback__label__value.isEnabled = enabled
        layout.SeekBarWithFeedback__seekbar.isEnabled = enabled
        super.setEnabled(enabled)
    }

    private fun createLabelText(labelTemplateStringResId:String,value:Int):String
    {
        return labelTemplateStringResId.replace("{}",value.toString())
    }

    private class Layout(parent:ViewGroup):LayoutContainer
    {
        override val containerView = parent.context.layoutInflater.inflate(R.layout.view__seek_bar_with_feedback,parent,true)
    }
}

package com.github.ericytsang.androidlib.seekbar

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import android.widget.SeekBar
import androidx.lifecycle.lifecycleScope
import com.github.ericytsang.androidlib.core.layoutInflater
import com.github.ericytsang.androidlib.seekbar.databinding.ViewSeekBarWithFeedbackBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class SeekBarWithFeedback(
    context:Context,
    attrs:AttributeSet
):LinearLayout(context,attrs)
{
    private val layout = ViewSeekBarWithFeedbackBinding.inflate(context.layoutInflater,this,true)

    val listener = mutableSetOf<Listener>()

    // region get/set progress

    private val progressListener = CompositeSeekBarChangedListener().also()
    {compositeSeekBarChangedListener ->
        layout.SeekBarWithFeedbackSeekbar.setOnSeekBarChangeListener(compositeSeekBarChangedListener)

        // notify listeners if there are any
        compositeSeekBarChangedListener.delegates += OnSeekBarProgressChangeListener()
        { _,_, fromUser ->
            listener.forEach()
            {
                it.onProgressChanged(
                    source = this@SeekBarWithFeedback,
                    progress = progress,
                    fromUser = fromUser
                )
            }
        }
    }

    var progress:Int
    get() = (layout.SeekBarWithFeedbackSeekbar.progress+min.value)*valueCoefficient.value
    set(value)
    {
        layout.SeekBarWithFeedbackSeekbar.progress = (value/valueCoefficient.value)-min.value
    }

    val progressFlow:Flow<Int> get() = callbackFlow()
    {
        trySendBlocking(progress)
        val listener = OnSeekBarProgressChangeListener { _,_,_ -> trySendBlocking(progress) }
        progressListener.delegates += listener
        awaitClose()
        {
            progressListener.delegates -= listener
        }
    }.buffer(CONFLATED)

    // endregion

    val valueCoefficient = MutableStateFlow(1)
    val labelTemplate = MutableStateFlow("{}")
    val min = MutableStateFlow(0)
    val max = MutableStateFlow(0)

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

    // region view coroutine scope

    private var coroutineScope = CoroutineScope(Dispatchers.Main)
    override fun onAttachedToWindow()
    {
        super.onAttachedToWindow()
        coroutineScope = CoroutineScope(Dispatchers.Main)
        coroutineScope.launch { setupListeners() }
    }

    override fun onDetachedFromWindow()
    {
        coroutineScope.cancel()
        super.onDetachedFromWindow()
    }

    private suspend fun setupListeners()
    {
        layout.lifecycleOwner?.lifecycleScope
        // updating min and max on the progress bar
        combine(min, max)
        { min, max ->
            layout.SeekBarWithFeedbackSeekbar.max = max-min
        }.collect()

        // updating text labels when needed
        combine(progressFlow,valueCoefficient,labelTemplate,min,max)
        { progressFlow,valueCoefficient,labelTemplate,min,max ->
            layout.SeekBarWithFeedbackLabelMin.text = createLabelText(
                labelTemplateStringResId = labelTemplate,
                value = min*valueCoefficient
            )
            layout.SeekBarWithFeedbackLabelMax.text = createLabelText(
                labelTemplateStringResId = labelTemplate,
                value = max*valueCoefficient
            )
            layout.SeekBarWithFeedbackLabelValue.text = createLabelText(
                labelTemplateStringResId = labelTemplate,
                value = progressFlow
            )
        }.collect()

        // adding listener to seekbar
        progressFlow.collect()
        { progressFlow ->
            layout.SeekBarWithFeedbackLabelValue.text = createLabelText(
                labelTemplateStringResId = labelTemplate.value,
                value = progressFlow
            )
        }

        // adding listeners to buttons
        layout.SeekBarWithFeedbackButtonDecrement.setOnClickListener()
        {
            layout.SeekBarWithFeedbackSeekbar.progress--
            listener.forEach()
            {
                it.onProgressChanged(this@SeekBarWithFeedback,progress,true)
            }
        }
        layout.SeekBarWithFeedbackButtonIncrement.setOnClickListener()
        {
            layout.SeekBarWithFeedbackSeekbar.progress++
            listener.forEach()
            {
                it.onProgressChanged(this@SeekBarWithFeedback,progress,true)
            }
        }
    }

    // endregion

    interface Listener
    {
        fun onProgressChanged(
            source:SeekBarWithFeedback,
            progress:Int,
            fromUser:Boolean
        )
    }

    override fun setEnabled(enabled:Boolean)
    {
        layout.SeekBarWithFeedbackLabelMin.isEnabled = enabled
        layout.SeekBarWithFeedbackLabelMax.isEnabled = enabled
        layout.SeekBarWithFeedbackLabelValue.isEnabled = enabled
        layout.SeekBarWithFeedbackSeekbar.isEnabled = enabled
        super.setEnabled(enabled)
    }

    private fun createLabelText(
        labelTemplateStringResId:String,
        value:Int
    ):String
    {
        return labelTemplateStringResId.replace("{}",value.toString())
    }
}

fun OnSeekBarProgressChangeListener(
    onProgressChanged:(seekBar:SeekBar,progress:Int,fromUser:Boolean) -> Unit
) = object:SeekBar.OnSeekBarChangeListener
{
    override fun onProgressChanged(seekBar:SeekBar,progress:Int,fromUser:Boolean)
    {
        onProgressChanged(seekBar,progress,fromUser)
    }
    override fun onStartTrackingTouch(seekBar:SeekBar) = Unit
    override fun onStopTrackingTouch(seekBar:SeekBar) = Unit
}

class CompositeSeekBarChangedListener(
    val delegates: MutableSet<SeekBar.OnSeekBarChangeListener> = mutableSetOf()
) : SeekBar.OnSeekBarChangeListener
{
    override fun onProgressChanged(seekBar:SeekBar,progress:Int,fromUser:Boolean)
    {
        delegates.forEach { it.onProgressChanged(seekBar, progress, fromUser) }
    }
    override fun onStartTrackingTouch(p0:SeekBar?)
    {
        delegates.forEach { it.onStartTrackingTouch(p0) }
    }
    override fun onStopTrackingTouch(p0:SeekBar?)
    {
        delegates.forEach { it.onStopTrackingTouch(p0) }
    }
}
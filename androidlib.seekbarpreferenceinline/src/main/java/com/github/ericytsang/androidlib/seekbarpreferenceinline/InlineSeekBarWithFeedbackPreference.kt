package com.github.ericytsang.androidlib.seekbarpreferenceinline

import android.content.Context
import android.util.AttributeSet
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.github.ericytsang.androidlib.seekbar.SeekBarWithFeedback
import com.github.ericytsang.lib.optional.Opt
import com.github.ericytsang.lib.prop.DataProp
import com.github.ericytsang.lib.prop.RaiiProp
import com.github.ericytsang.lib.prop.value
import java.util.concurrent.LinkedBlockingQueue

class InlineSeekBarWithFeedbackPreference(
        context:Context,
        attrs:AttributeSet)
    :Preference(context,attrs)
{
    init
    {
        layoutResource = R.layout.pref__inline_seek_bar_with_feedback_dialog
    }

    // fetch & validate configurations from XML

    private val typedArray = context.theme.obtainStyledAttributes(attrs,R.styleable.InlineSeekBarWithFeedbackPreference,0,0)
    private val minSliderValue = typedArray.getInt(R.styleable.InlineSeekBarWithFeedbackPreference_min,Integer.MAX_VALUE)
    private val maxSliderValue = typedArray.getInt(R.styleable.InlineSeekBarWithFeedbackPreference_max,Integer.MIN_VALUE)
    private val labelTemplate = typedArray.getString(R.styleable.InlineSeekBarWithFeedbackPreference_labelTemplate)?:"{}"
    init
    {
        require(minSliderValue < maxSliderValue)
        require(minSliderValue != Integer.MAX_VALUE)
        require(maxSliderValue != Integer.MIN_VALUE)
    }

    // apply configurations from XML to view

    private var seekBar:SeekBarWithFeedback? = null
    private val configurations = LinkedBlockingQueue<(SeekBarWithFeedback)->Unit>()

    override fun onBindViewHolder(holder:PreferenceViewHolder)
    {
        super.onBindViewHolder(holder)
        seekBar = holder.findViewById(R.id.seek_bar) as SeekBarWithFeedback
        seekBar!!.min.value = minSliderValue
        seekBar!!.max.value = maxSliderValue
        seekBar!!.labelTemplate.value = labelTemplate
        configure {}
    }

    // apply user-supplied configurations to view

    fun configure(block:(SeekBarWithFeedback)->Unit)
    {
        configurations += block

        val seekBar = seekBar
        if (seekBar != null)
        {
            generateSequence {configurations.poll()}.forEach {it(seekBar)}
        }
    }
}

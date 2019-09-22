package com.github.ericytsang.androidlib.seekbarpreferenceinline

import android.content.Context
import android.util.AttributeSet
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.github.ericytsang.androidlib.core.cast
import com.github.ericytsang.androidlib.seekbar.SeekBarWithFeedback
import com.github.ericytsang.lib.closeablegroup.CloseableGroup
import com.github.ericytsang.lib.optional.Opt
import com.github.ericytsang.lib.prop.RaiiProp
import com.github.ericytsang.lib.prop.value
import java.io.Closeable

class InlineSeekBarWithFeedbackPreference(
        context:Context,
        attrs:AttributeSet)
    :Preference(context,attrs)
{
    // parse XML attributes
    private val minSliderValue:Int
    private val maxSliderValue:Int
    private val labelTemplate:String
    private val persistenceStrategy:PersistenceStrategy
    init
    {
        context.theme.obtainStyledAttributes(attrs,R.styleable.InlineSeekBarWithFeedbackPreference,0,0).apply()
        {
            try
            {
                minSliderValue = getInt(R.styleable.InlineSeekBarWithFeedbackPreference_min,Integer.MAX_VALUE)
                maxSliderValue = getInt(R.styleable.InlineSeekBarWithFeedbackPreference_max,Integer.MIN_VALUE)
                labelTemplate = getString(R.styleable.InlineSeekBarWithFeedbackPreference_labelTemplate)?:"{}"
                persistenceStrategy = context::class.java.classLoader!!
                        .loadClass(getString(R.styleable.InlineSeekBarWithFeedbackPreference_persistenceStrategy))
                        .newInstance()
                        .cast()
            }
            finally
            {
                recycle()
            }
        }
        require(minSliderValue < maxSliderValue)
        require(minSliderValue != Integer.MAX_VALUE)
        require(maxSliderValue != Integer.MIN_VALUE)
    }

    // set preference layout
    init
    {
        layoutResource = R.layout.pref__inline_seek_bar_with_feedback
    }




    /* view lifecycle */

    private val attached = RaiiProp(Opt.none<Attached>())

    override fun onBindViewHolder(holder:PreferenceViewHolder)
    {
        super.onBindViewHolder(holder)
        attached.value = {Opt.some(Attached(this,holder))}
    }

    override fun onDetached()
    {
        attached.value = {Opt.none()}
        super.onDetached()
    }




    /* classes & interfaces */

    interface PersistenceStrategy
    {
        fun load(context:Context,block:((Int)->Unit)):Closeable
        fun save(context:Context,value:Int)
    }

    private class Attached(
            val preference:InlineSeekBarWithFeedbackPreference,
            holder:PreferenceViewHolder)
        :Closeable
    {
        private val closeables = CloseableGroup()
        override fun close() = closeables.close()

        val seekBar = holder.findViewById(R.id.seek_bar) as SeekBarWithFeedback

        init
        {
            seekBar.min.value = preference.minSliderValue
            seekBar.max.value = preference.maxSliderValue
            seekBar.labelTemplate.value = preference.labelTemplate

            closeables.addCloseables()
            {
                addCloseablesScope ->
                addCloseablesScope+preference.persistenceStrategy.load(preference.context)
                {
                    newValue ->
                    if (seekBar.progress.value != newValue)
                    {
                        seekBar.progress.value = newValue
                    }
                }
            }
            seekBar.listener = object:SeekBarWithFeedback.Listener
            {
                override fun onProgressChanged(source:SeekBarWithFeedback,progress:Int,fromUser:Boolean)
                {
                    if (fromUser)
                    {
                        preference.persistenceStrategy.save(preference.context,progress)
                    }
                }
            }
        }
    }
}

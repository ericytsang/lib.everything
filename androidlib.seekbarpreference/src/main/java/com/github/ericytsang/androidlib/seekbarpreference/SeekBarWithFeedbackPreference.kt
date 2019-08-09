package com.github.ericytsang.androidlib.seekbarpreference

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.github.ericytsang.androidlib.core.context.wrap
import com.github.ericytsang.androidlib.core.postOnUiThread
import com.github.ericytsang.lib.noopclose.NoopClose
import com.github.ericytsang.lib.optional.Opt
import com.github.ericytsang.lib.prop.RaiiProp
import com.github.ericytsang.lib.prop.listen
import com.github.ericytsang.lib.prop.value
import java.io.Closeable

class SeekBarWithFeedbackPreference(
        context:Context,
        attrs:AttributeSet)
    :Preference(context,attrs)
{
    private val attached = RaiiProp(Opt.none<Attached>())
    private val persistenceStrategy = RaiiProp(Opt.none<NoopClose<PersistenceStrategy>>())
    private val running = RaiiProp(Opt.none<Running>())

    private val runningAssigner = listOf(persistenceStrategy,attached).listen()
    {
        val attached = attached.value.invoke().opt
        val persistenceStrategy = persistenceStrategy.value.invoke().opt?.wrapee
        if (attached != null && persistenceStrategy != null)
        {
            running.value = {Opt.some(Running(attached,persistenceStrategy))}
        }
        else
        {
            running.value = {Opt.none()}
        }
    }

    // applying configuratios from XML

    private val typedArray = context.theme.obtainStyledAttributes(attrs,R.styleable.SeekBarWithFeedbackPreference,0,0)
    private val minSliderValue = typedArray.getInt(R.styleable.SeekBarWithFeedbackPreference_min,Integer.MAX_VALUE)
    private val maxSliderValue = typedArray.getInt(R.styleable.SeekBarWithFeedbackPreference_max,Integer.MIN_VALUE)
    private val labelTemplate = typedArray.getString(R.styleable.SeekBarWithFeedbackPreference_labelTemplate)?:"{}"
    init
    {
        typedArray.recycle()
        require(minSliderValue < maxSliderValue)
        require(minSliderValue != Integer.MAX_VALUE)
        require(maxSliderValue != Integer.MIN_VALUE)
    }

    // lifecycle

    override fun onBindViewHolder(holder:PreferenceViewHolder)
    {
        super.onBindViewHolder(holder)
        attached.value = {Opt.some(Attached(this))}
    }

    override fun onDetached()
    {
        attached.value = {Opt.none()}
        persistenceStrategy.value = {Opt.none()}
        running.value = {Opt.none()}
        runningAssigner.close()
        super.onDetached()
    }

    override fun onClick()
    {
        super.onClick()
        running.value.invoke().opt?.onClick()
    }

    // PersistenceStrategy

    fun setPersistenceStrategy(newPersistenceStrategy:PersistenceStrategy)
    {
        persistenceStrategy.value = {Opt.some(NoopClose(newPersistenceStrategy))}
    }

    interface PersistenceStrategy
    {
        val save:(context:Context,dip:Int)->Unit
        fun load(context:Context,onSaved:(context:Context,dip:Int)->Unit):Closeable
    }

    // Raii

    private class Attached(
            val sliderPreference:SeekBarWithFeedbackPreference)
        :Closeable
    {
        override fun close() = Unit
        fun setSummary(pixels:Int)
        {
            postOnUiThread()
            {
                sliderPreference.summary = sliderPreference.labelTemplate.replace("{}",pixels.toString())
            }
        }
    }

    private class Running(
            val attached:Attached,
            val persistenceStrategy:PersistenceStrategy)
        :Closeable
    {
        private var oldValue = 0
        private val listener = persistenceStrategy.load(attached.sliderPreference.context)
        {
            _,dip->
            oldValue = dip
            attached.setSummary(dip)
        }

        override fun close()
        {
            listener.close()
        }

        fun onClick()
        {
            SeekBarWithFeedbackDialog.start(
                    attached.sliderPreference.context.let {it as Activity}.wrap(),
                    SeekBarWithFeedbackDialog.Params(
                            attached.sliderPreference.title.toString(),
                            oldValue,
                            attached.sliderPreference.minSliderValue,
                            attached.sliderPreference.maxSliderValue,
                            attached.sliderPreference.labelTemplate,
                            persistenceStrategy.save))
        }
    }
}

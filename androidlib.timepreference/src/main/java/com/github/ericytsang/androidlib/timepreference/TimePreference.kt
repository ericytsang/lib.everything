package com.github.ericytsang.androidlib.timepreference

import android.content.Context
import android.util.AttributeSet
import com.github.ericytsang.lib.optional.Opt
import org.joda.time.LocalTime

class TimePreference(
        context:Context,
        attrs:AttributeSet)
    :AbstractTimePreference<Opt<LocalTime>>(
        context,
        attrs,
        Strategy())
{
    private class Strategy:AbstractTimePreference.Strategy<Opt<LocalTime>>
    {
        override fun toPreferenceValue(customValue:Opt<LocalTime>):Selection<Opt<LocalTime>> = when(customValue)
        {
            is Opt.Some -> Selection.Time(customValue.opt)
            is Opt.None -> Selection.Clear()
        }
        override fun fromTime(time:LocalTime?):Opt<LocalTime> = Opt.of(time)
        override val customAction:CustomAction<Opt<LocalTime>>? = null
    }
}

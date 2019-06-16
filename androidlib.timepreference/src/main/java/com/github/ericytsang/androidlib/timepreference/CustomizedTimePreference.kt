package com.github.ericytsang.androidlib.timepreference

import android.content.Context
import android.util.AttributeSet
import com.github.ericytsang.androidlib.core.usingAttrs
import org.joda.time.LocalTime

class CustomizedTimePreference(
        context:Context,
        attributeSet:AttributeSet)
    :
        AbstractTimePreference<CustomizedTimePreference.Strategy.Selection>(
                context,
                attributeSet,
                Strategy(context,attributeSet))
{
    class Strategy(
            context:Context,
            attrs:AttributeSet)
        :AbstractTimePreference.Strategy<Strategy.Selection>
    {
        override fun fromTime(time:LocalTime?) = when (time)
        {
            null -> Selection.Clear()
            else -> Selection.Time(time)
        }

        override val customAction = object:CustomAction<Selection>
        {
            override fun customValue() = Selection.Custom()
            override val buttonText:String = context.usingAttrs(attrs,R.styleable.CustomizedTimePreference)
            {
                it.getString(R.styleable.CustomizedTimePreference_customButtonText)!!
            }
        }

        sealed class Selection
        {
            data class Time(val time:LocalTime):Selection()
            class Clear(val unit:Unit = Unit):Selection()
            class Custom(val unit:Unit = Unit):Selection()
        }
    }
}

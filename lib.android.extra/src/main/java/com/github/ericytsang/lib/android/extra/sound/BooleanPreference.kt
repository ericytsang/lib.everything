package com.github.ericytsang.lib.android.extra.sound

import android.content.SharedPreferences
import com.github.ericytsang.lib.android.editAndCommit
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class BooleanPreference(
        val sharedPreferencesKey:String,
        sharedPreferences:SharedPreferences,
        val defaultValue:Boolean)
    :GeneralPreference<Boolean>(sharedPreferences)
{
    override fun getValue(sharedPreferences:SharedPreferences):Boolean
    {
        return sharedPreferences.getBoolean(sharedPreferencesKey,defaultValue)
    }

    override fun setValue(sharedPreferencesEditor:SharedPreferences.Editor,value:Boolean)
    {
        sharedPreferencesEditor.putBoolean(sharedPreferencesKey,value)
    }
}

abstract class GeneralPreference<Value:Any>(
        private val sharedPreferences:SharedPreferences)
    :ReadWriteProperty<Any,Value>
{
    final override fun getValue(thisRef:Any,property:KProperty<*>):Value
    {
        return getValue(sharedPreferences)
    }

    final override fun setValue(thisRef:Any,property:KProperty<*>,value:Value)
    {
        sharedPreferences.editAndCommit {
            setValue(it,value)
        }
    }

    abstract fun getValue(sharedPreferences:SharedPreferences):Value
    abstract fun setValue(sharedPreferencesEditor:SharedPreferences.Editor,value:Value)
}

object GeneralPreference2
{
    interface ReadOnly<Source:Any,Value:Any>
    {
        val preferece:Value
        val listeners:Set<(source:Source,old:Value,new:Value)->Unit>
    }
    interface ReadWrite<Source:Any,Value:Any>:ReadOnly<Source,Value>
    {
        override var preferece:Value

        abstract class Impl<Source:Any,Value:Any>(val source:Source):ReadWrite<Source,Value>
        {
            final override val listeners = mutableSetOf<(source:Source,old:Value,new:Value)->Unit>()

            final override var preferece:Value
                get() = _preferece
                set(value)
                {
                    val old = _preferece
                    _preferece = value
                    listeners.forEach {it(source,old,value)}
                }

            protected abstract var _preferece:Value
        }
    }
}

abstract class BooleanPreference2<Source:Any>(
        source:Source,
        val sharedPreferencesKey:String,
        val sharedPreferences:SharedPreferences,
        val defaultValue:Boolean)
    :GeneralPreference2.ReadWrite.Impl<Source,Boolean>(source)
{
    final override var _preferece:Boolean
        get()
        {
            return sharedPreferences.getBoolean(sharedPreferencesKey,defaultValue)
        }
        set(value)
        {
            sharedPreferences.editAndCommit {
                it.putBoolean(sharedPreferencesKey,value)
            }
        }
}

package com.github.ericytsang.androidlib.twolinelistpreference

import android.content.Context
import android.util.AttributeSet
import androidx.preference.DialogPreference

class TwoLineListPreference(
        context:Context,
        attrs:AttributeSet)
    :DialogPreference(context,attrs)
{
    // fetch & validate configurations from XML
    val listItems = run()
    {
        val typedArray = context.theme.obtainStyledAttributes(attrs,R.styleable.TwoLineListPreference,0,0)
        val titles = typedArray.getTextArray(R.styleable.TwoLineListPreference_entries)
        val descriptions = typedArray.getTextArray(R.styleable.TwoLineListPreference_entryDescriptions)
        val values = typedArray.getTextArray(R.styleable.TwoLineListPreference_entryValues)
        typedArray.recycle()

        require(titles.size == descriptions.size)
        require(titles.size == values.size)

        titles.indices.map {ListItem(
                titles[it].toString(),
                descriptions[it].toString(),
                values[it].toString())}
    }

    var value:String = ""
        set(value)
        {
            if (field != value) {
                field = value
                summary = listItems.find {it.value == value}?.header?:""
                persistString(value)
                notifyChanged()
            }
        }

    override fun onSetInitialValue(defaultValue:Any?)
    {
        value = getPersistedString(defaultValue as String)
    }

    data class ListItem(
            var header:String,
            var description:String,
            var value:String)
    {
        init
        {
            require(value.isNotBlank())
            require(header.isNotBlank())
        }
    }
}

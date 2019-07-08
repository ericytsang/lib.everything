package com.github.ericytsang.androidlib.timepreference

import android.content.Context
import android.content.res.TypedArray
import android.os.Build
import android.os.Bundle
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import android.widget.TimePicker
import androidx.preference.DialogPreference
import androidx.preference.PreferenceDialogFragmentCompat
import com.github.ericytsang.androidlib.core.getStringCompat
import com.github.ericytsang.androidlib.dialogpreference.DialogPreferenceHelper
import com.github.ericytsang.lib.closeablegroup.CloseableGroup
import com.github.ericytsang.lib.prop.DataProp
import com.github.ericytsang.lib.prop.listen
import com.github.ericytsang.lib.prop.value
import org.joda.time.LocalTime
import org.joda.time.format.DateTimeFormat
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

abstract class AbstractTimePreference<T:Any>(
        context:Context,
        attrs:AttributeSet,
        val strategy:Strategy<T>)
    :
        DialogPreference(context,attrs),
        DialogPreferenceHelper.DialogFactory
{
    companion object
    {
        const val CLEAR_TIME_AS_INT = Int.MIN_VALUE
        const val CUSTOM_TIME_AS_INT = Int.MAX_VALUE
    }

    private val closeables = CloseableGroup()
    override fun onDetached()
    {
        closeables.close()
        super.onDetached()
    }

    private val _selectedTime = DataProp<Selection<T>>(Selection.Clear())
    val selectedTime = DataProp(_selectedTime.value.value)

    init
    {
        val isBeingSetViaListener = ReentrantLock()

        // selectedTime is value of _selectedTime
        closeables += listOf(_selectedTime).listen()
        {
            if (isBeingSetViaListener.isHeldByCurrentThread.not())
            {
                isBeingSetViaListener.withLock()
                {
                    selectedTime.value = _selectedTime.value.value
                }
            }
        }

        // allow callers to set selectedTime, which will cascade and set _selectedTime
        closeables += listOf(selectedTime).listen()
        {
            if (isBeingSetViaListener.isHeldByCurrentThread.not())
            {
                isBeingSetViaListener.withLock()
                {
                    _selectedTime.value = strategy.toPreferenceValue(selectedTime.value)
                }
            }
        }
    }

    // update summary when times are set
    init
    {
        closeables += listOf(_selectedTime).listen()
        {
            summary = when(val selectedValue = _selectedTime.value)
            {
                is Selection.Time -> DateTimeFormat.shortTime().print(selectedValue.localTime)
                is Selection.Custom -> selectedValue.customAction.buttonText
                is Selection.Clear -> context.getStringCompat(R.string.not_set)
            }
        }
    }

    // update persisted value when times are set
    init
    {
        closeables += listOf(_selectedTime).listen(false)
        {
            val persistedInt = when(val selectedValue = _selectedTime.value)
            {
                is Selection.Time -> selectedValue.localTime.millisOfDay
                is Selection.Custom -> CUSTOM_TIME_AS_INT
                is Selection.Clear -> CLEAR_TIME_AS_INT
            }
            persistInt(persistedInt)
        }
    }

    // set layout resource of dialog
    init
    {
        dialogLayoutResource = R.layout.dialog_pref__time_picker
    }

    // hide the default positive and negative buttons
    init
    {
        positiveButtonText = ""
        negativeButtonText = ""
    }

    private fun setTime(localTime:LocalTime?)
    {
        val value = strategy.fromTime(localTime)
        if (callChangeListener(value))
        {
            _selectedTime.value = localTime?.let {Selection.Time<T>(it)}?:Selection.Clear()
        }
    }

    private fun setCustom()
    {
        val customAction = strategy.customAction!!
        val value = customAction.customValue()
        if (callChangeListener(value))
        {
            _selectedTime.value = Selection.Custom(customAction)
        }
    }

    override fun onGetDefaultValue(a:TypedArray,index:Int):Int
    {
        return a.getInt(index,CLEAR_TIME_AS_INT)
    }

    override fun onSetInitialValue(defaultValue:Any?)
    {
        val nonNullDefaultValue = defaultValue
                .let {it as? Int}
                ?:CLEAR_TIME_AS_INT

        val persistedInt = if (isPersistent)
        {
            getPersistedInt(nonNullDefaultValue)
        }
        else
        {
            nonNullDefaultValue
        }

        _selectedTime.value = when(persistedInt)
        {
            CLEAR_TIME_AS_INT -> Selection.Clear()
            CUSTOM_TIME_AS_INT -> Selection.Custom(strategy.customAction!!)
            else -> Selection.Time(LocalTime.fromMillisOfDay(persistedInt.toLong()))
        }
    }

    override fun newDialog(preferenceKey:String):PreferenceDialogFragmentCompat
    {
        return Dialog.newInstance(preferenceKey)
    }

    private val Selection<T>.value:T get() = when(this)
    {
        is Selection.Time -> strategy.fromTime(localTime)
        is Selection.Custom -> customAction.customValue()
        is Selection.Clear -> strategy.fromTime(null)
    }

    sealed class Selection<T:Any>
    {
        data class Time<T:Any>(val localTime:LocalTime):Selection<T>()
        data class Custom<T:Any>(val customAction:CustomAction<T>):Selection<T>()
        class Clear<T:Any>:Selection<T>()
    }

    interface Strategy<T:Any>
    {
        fun toPreferenceValue(customValue:T):Selection<T>
        fun fromTime(time:LocalTime?):T
        val customAction:CustomAction<T>?
    }

    interface CustomAction<out T:Any>
    {
        fun customValue():T
        val buttonText:String
    }

    class Dialog:PreferenceDialogFragmentCompat()
    {

        companion object
        {
            fun newInstance(key:String):Dialog
            {
                val fragment = Dialog()
                val b = Bundle(0)
                b.putString(ARG_KEY,key)
                fragment.arguments = b
                return fragment
            }
        }

        override fun getPreference():AbstractTimePreference<*>
        {
            return super.getPreference() as AbstractTimePreference<*>
        }

        override fun onBindDialogView(view:View)
        {
            super.onBindDialogView(view)
            val picker = view.findViewById<TimePicker>(R.id.time_picker)
            val customButton:Button = view.findViewById(R.id.button__custom)
            val clearButton:Button = view.findViewById(R.id.button__clear)
            val okButton:Button = view.findViewById(R.id.button__ok)

            // picker time reflects the local time
            run()
            {
                val timeToShowInPicker = when(val selectedValue = preference._selectedTime.value)
                {
                    is Selection.Time -> selectedValue.localTime
                    is Selection.Custom -> LocalTime.now()
                    is Selection.Clear -> LocalTime.now()
                }
                picker.hourCompat = timeToShowInPicker.hourOfDay
                picker.minuteCompat = timeToShowInPicker.minuteOfHour
            }

            // determine whether we should show the custom button
            val customAction = preference.strategy.customAction
            customButton.visibility =  if (customAction != null)
            {
                customButton.text = customAction.buttonText
                View.VISIBLE
            }
            else
            {
                View.GONE
            }

            // custom button tells preference that user has selected the custom value
            customButton.setOnClickListener()
            {
                preference.setCustom()
                dialog!!.dismiss()
            }

            // clear button clears time, and closes dialog
            clearButton.setOnClickListener()
            {
                preference.setTime(null)
                dialog!!.dismiss()
            }

            // ok button sets time, and closes dialog
            okButton.setOnClickListener()
            {
                val selectedTime = LocalTime.MIDNIGHT
                        .withHourOfDay(picker.hourCompat)
                        .withMinuteOfHour(picker.minuteCompat)
                preference.setTime(selectedTime)
                dialog!!.dismiss()
            }
        }

        override fun onDialogClosed(positiveResult:Boolean) = Unit

        private var TimePicker.hourCompat
            get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) hour else @Suppress("DEPRECATION") currentHour
            set(value)
            {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                    hour = value
                else
                    @Suppress("DEPRECATION")
                    currentHour = value
            }

        private var TimePicker.minuteCompat
            get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) minute else @Suppress("DEPRECATION") currentMinute
            set(value)
            {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                    minute = value
                else
                    @Suppress("DEPRECATION")
                    currentMinute = value
            }
    }
}
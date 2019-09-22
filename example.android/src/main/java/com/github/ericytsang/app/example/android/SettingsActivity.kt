package com.github.ericytsang.app.example.android

import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.github.ericytsang.androidlib.colorpreference.ColorPreference
import com.github.ericytsang.androidlib.core.activity.ContextCompanionWithStart
import com.github.ericytsang.androidlib.core.kClass
import com.github.ericytsang.androidlib.core.context.TypedContext.BackgroundContext.ForegroundContext
import com.github.ericytsang.androidlib.core.getStringCompat
import com.github.ericytsang.androidlib.core.intent.StartableIntent.StartableForegroundIntent.ActivityIntent
import com.github.ericytsang.androidlib.core.postOnUiThread
import com.github.ericytsang.androidlib.seekbarpreferenceinline.InlineSeekBarWithFeedbackPreference
import com.github.ericytsang.lib.closeablegroup.CloseableGroup
import com.github.ericytsang.lib.optional.Opt
import com.github.ericytsang.lib.prop.DataProp
import com.github.ericytsang.lib.prop.RaiiProp
import com.github.ericytsang.lib.prop.listen
import com.github.ericytsang.lib.prop.mutableNullableValue
import com.github.ericytsang.lib.prop.value
import java.io.Closeable

class SettingsActivity:AppCompatActivity()
{
    companion object:ContextCompanionWithStart<SettingsActivity,ForegroundContext,SUnit,ActivityIntent>(ActivityIntent)
    {
        override val contextClass get() = kClass<SettingsActivity>()
        override val paramsClass get() = kClass<SUnit>()
    }

    override fun onCreate(savedInstanceState:Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity__settings)
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings,SettingsFragment())
                .commit()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item:MenuItem):Boolean
    {
        return when(item.itemId)
        {
            android.R.id.home ->
            {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    class SettingsFragment:PreferenceFragmentCompat()
    {
        override fun onCreatePreferences(savedInstanceState:Bundle?,rootKey:String?)
        {
            setPreferencesFromResource(R.xml.root_preferences,rootKey)
        }

        private val started = RaiiProp(Opt.none<Started>())

        override fun onStart()
        {
            super.onStart()
            started.mutableNullableValue = {Started(this)}
        }

        override fun onStop()
        {
            started.close()
            super.onStop()
        }
    }




    /* started */

    private class Started(val fragment:SettingsFragment):Closeable
    {
        private val closeableGroup = CloseableGroup()
        override fun close() = closeableGroup.close()

        // show the last saved value
        init
        {
            fragment.findPreference<Preference>(R.string.pref_key__save).also()
            {
                preference ->
                closeableGroup.addCloseables()
                {
                    addCloseablesScope ->
                    addCloseablesScope+listOf(IntPersistenceStrategy.saveMessage).listen()
                    {
                        preference.summary = IntPersistenceStrategy.saveMessage.value
                    }
                }
            }
        }

        // show the last loaded value
        init
        {
            fragment.findPreference<Preference>(R.string.pref_key__load).also()
            {
                preference ->
                closeableGroup.addCloseables()
                {
                    addCloseablesScope ->
                    addCloseablesScope+listOf(IntPersistenceStrategy.loadMessage).listen()
                    {
                        preference.summary = IntPersistenceStrategy.loadMessage.value
                    }
                }
            }
        }

        // todo: move to library module
        private fun <T:Preference> PreferenceFragmentCompat.findPreference(@StringRes stringResId:Int):T
        {
            return findPreference(activity!!.getStringCompat(stringResId))!!
        }
    }




    /* persistence strategies */

    class ColorPersistenceStrategy:ColorPreference.PersistenceStrategy
    {
        private val intPersistenceStrategy = IntPersistenceStrategy()
        override fun save(context:Context,newColor:Int) = intPersistenceStrategy.save(newColor)
        override fun load(context:Context,block:(Int)->Unit) = intPersistenceStrategy.load(block)
    }

    class SeekBarPersistenceStrategy:InlineSeekBarWithFeedbackPreference.PersistenceStrategy
    {
        private val intPersistenceStrategy = IntPersistenceStrategy()
        override fun save(context:Context,value:Int) = intPersistenceStrategy.save(value)
        override fun load(context:Context,block:(Int)->Unit) = intPersistenceStrategy.load(block)
    }

    private class IntPersistenceStrategy
    {
        companion object
        {
            val saveMessage = DataProp("")
            val loadMessage = DataProp("")
        }
        private val value = DataProp(0)
        fun save(newValue:Int)
        {
            value.value = newValue
            postOnUiThread()
            {
                saveMessage.value = newValue.toString()
            }
        }

        fun load(block:(Int)->Unit):Closeable
        {
            return listOf(value).listen()
            {
                block(value.value)
                postOnUiThread()
                {
                    loadMessage.value = value.value.toString()
                }
            }
        }
    }
}
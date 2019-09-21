package com.github.ericytsang.app.example.android

import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat
import com.github.ericytsang.androidlib.colorpreference.ColorPreference
import com.github.ericytsang.androidlib.core.activity.ContextCompanionWithStart
import com.github.ericytsang.androidlib.core.kClass
import com.github.ericytsang.androidlib.core.context.TypedContext.BackgroundContext.ForegroundContext
import com.github.ericytsang.androidlib.core.intent.StartableIntent.StartableForegroundIntent.ActivityIntent
import com.github.ericytsang.lib.prop.DataProp
import com.github.ericytsang.lib.prop.listen
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
    }




    /* persistence strategies */

    class ColorPersistenceStrategy:ColorPreference.PersistenceStrategy
    {
        private val intPersistenceStrategy = IntPersistenceStrategy()
        override fun save(context:Context,newColor:Int) = intPersistenceStrategy.save(context, newColor)
        override fun load(context:Context,block:(Int)->Unit) = intPersistenceStrategy.load(context, block)
    }


    private class IntPersistenceStrategy
    {
        private val color = DataProp(0)
        fun save(context:Context,value:Int)
        {
            color.value = value
        }

        fun load(context:Context,block:(Int)->Unit):Closeable
        {
            return listOf(color).listen()
            {
                block(color.value)
            }
        }
    }
}
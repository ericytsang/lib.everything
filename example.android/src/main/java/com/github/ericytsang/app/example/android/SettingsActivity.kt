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
        private var color:Int = 0

        override fun save(context:Context,newColor:Int)
        {
            color = newColor
        }

        override fun load(context:Context):Int
        {
            return color
        }
    }
}
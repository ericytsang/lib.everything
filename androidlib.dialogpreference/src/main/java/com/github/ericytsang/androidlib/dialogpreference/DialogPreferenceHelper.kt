package com.github.ericytsang.androidlib.dialogpreference

import androidx.preference.Preference
import androidx.preference.PreferenceDialogFragmentCompat
import androidx.preference.PreferenceFragmentCompat

object DialogPreferenceHelper
{
    fun onDisplayPreferenceDialog(
            preferenceFragment:PreferenceFragmentCompat,
            preference:Preference,
            superOnDisplayPreferenceDialog:(Preference)->Unit)
    {
        val dialogFragment = when(preference)
        {
            is DialogFactory -> preference.newDialog(preference.key)
            else -> null
        }
        if (dialogFragment != null)
        {
            dialogFragment.setTargetFragment(preferenceFragment,0)
            dialogFragment.show(preferenceFragment.fragmentManager!!,dialogFragment::class.qualifiedName!!)
        }
        else
        {
            superOnDisplayPreferenceDialog(preference)
        }
    }

    interface DialogFactory
    {
        fun newDialog(preferenceKey:String):PreferenceDialogFragmentCompat
    }
}

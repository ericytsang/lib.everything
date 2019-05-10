package com.github.ericytsang.androidlib.twolinelistpreference

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.CheckedTextView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.preference.PreferenceDialogFragmentCompat
import com.github.ericytsang.androidlib.core.layoutInflater

class TwoLineListPreferenceDialog:PreferenceDialogFragmentCompat()
{
    companion object
    {
        fun newInstance(key:String):TwoLineListPreferenceDialog
        {
            val fragment = TwoLineListPreferenceDialog()
            val b = Bundle(1)
            b.putString(ARG_KEY,key)
            fragment.arguments = b
            return fragment
        }
    }

    private var selectedValue:String? = null

    private val listAdapter = object:BaseAdapter()
    {
        override fun getView(position:Int,convertView:View?,parent:ViewGroup):View
        {
            // inflate view
            val view = convertView ?: parent.context.layoutInflater.inflate(
                    R.layout.list_item,parent,false)

            // populate views with data
            val data = getItem(position)
            val text1 = view.findViewById<TextView>(R.id.text1)
            val text2 = view.findViewById<TextView>(R.id.text2)
            val radiobutton = view.findViewById<CheckedTextView>(R.id.radiobutton)
            text1.text = data.header
            if (data.description.isNotBlank())
            {
                text2.text = data.description
                text2.visibility = View.VISIBLE
            }
            else
            {
                text2.visibility = View.GONE
            }
            radiobutton.isChecked = preference.value == data.value

            return view
        }
        override fun getItem(position:Int) = preference.listItems[position]
        override fun getItemId(position:Int) = position.toLong()
        override fun getCount() = preference.listItems.size
    }

    override fun onPrepareDialogBuilder(builder:AlertDialog.Builder)
    {
        super.onPrepareDialogBuilder(builder)

        builder.setSingleChoiceItems(
                listAdapter,
                preference.listItems.indexOfFirst {it.value == preference.value})
        {
            dialog,which ->
            selectedValue = preference.listItems[which].value

            /*
             * Clicking on an item simulates the positive button
             * click, and dismisses the dialog.
             */
            onClick(dialog,DialogInterface.BUTTON_POSITIVE)
            dialog.dismiss()
        }

        /*
         * The typical interaction for list-based dialogs is to have
         * click-on-an-item dismiss the dialog instead of the user having to
         * press 'Ok'.
         */
        builder.setPositiveButton(null,null)
    }

    override fun onDialogClosed(positiveResult:Boolean)
    {
        val selectedValue = selectedValue
        if (positiveResult && selectedValue != null)
        {
            if (preference.callChangeListener(selectedValue))
            {
                preference.value = selectedValue
            }
        }
    }

    // stateless methods

    override fun getPreference():TwoLineListPreference
    {
        return super.getPreference() as TwoLineListPreference
    }
}

package com.github.ericytsang.androidlib.colorpreference

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.View
import android.view.WindowManager
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.flask.colorpicker.ColorPickerView
import com.flask.colorpicker.builder.ColorPickerDialogBuilder
import com.github.ericytsang.androidlib.core.cast
import com.github.ericytsang.androidlib.core.getDrawableCompat
import com.github.ericytsang.lib.optional.Opt
import com.github.ericytsang.lib.prop.RaiiProp
import com.github.ericytsang.lib.prop.value
import java.io.Closeable

// todo: add licenses to the app
class ColorPreference(
        context:Context,
        attrs:AttributeSet)
    :Preference(context,attrs)
{
    // parse XML attributes
    private val showAlphaSlider:Boolean
    private val persistenceStrategy:PersistenceStrategy
    init
    {
        context.theme.obtainStyledAttributes(attrs,R.styleable.ColorPreference,0,0).apply()
        {
            try
            {
                showAlphaSlider = getBoolean(R.styleable.ColorPreference_showAlphaSlider,true)
                persistenceStrategy = context::class.java.classLoader!!
                        .loadClass(getString(R.styleable.ColorPreference_persistenceStrategy))
                        .newInstance()
                        .cast()
            }
            finally
            {
                recycle()
            }
        }
    }

    // set preference layout
    init
    {
        widgetLayoutResource = R.layout.layout__swatch
    }




    /* view lifecycle */

    private val attached = RaiiProp(Opt.none<Attached>())

    override fun onBindViewHolder(holder:PreferenceViewHolder)
    {
        super.onBindViewHolder(holder)
        attached.value = {Opt.some(Attached(this,holder))}
    }

    override fun onDetached()
    {
        attached.value = {Opt.none()}
        super.onDetached()
    }




    /* event handling */

    override fun onClick()
    {
        super.onClick()
        attached.value.invoke().opt?.onClick()
    }




    /* classes & interfaces */

    interface PersistenceStrategy
    {
        fun save(context:Context,newColor:Int)
        fun load(context:Context):Int
    }

    private class Attached(
            val colorPreference:ColorPreference,
            holder:PreferenceViewHolder)
        :Closeable
    {
        val previewView = holder.itemView.findViewById<View>(R.id.color_sample)!!
        val checkeredBackgroundView = holder.itemView.findViewById<View>(R.id.checkered_background)!!

        // configure preview color background
        init
        {
            checkeredBackgroundView.background = TilingDrawable(
                    colorPreference.context.getDrawableCompat(R.drawable.checkers),
                    TilingDrawable.RepeatMode.FROM_CENTER)
        }

        // show preview color
        init
        {
            val color = colorPreference.persistenceStrategy.load(colorPreference.context)
            setPreviewColor(color)
        }

        override fun close() = Unit

        fun setPreviewColor(newColor:Int)
        {
            val background = previewView.background as GradientDrawable
            background.setColor(newColor)
        }

        fun onClick()
        {
            var selectedColor = colorPreference.persistenceStrategy.load(colorPreference.context)
            val setColor = fun(newColor:Int)
            {
                colorPreference.persistenceStrategy.save(colorPreference.context,newColor)
                setPreviewColor(newColor)
            }
            ColorPickerDialogBuilder
                    .with(colorPreference.context)
                    .setTitle(colorPreference.title.toString())
                    .initialColor(selectedColor)
                    .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                    .density(12)
                    .setOnColorSelectedListener {setColor(it)}
                    .setOnColorChangedListener {setColor(it)}
                    .setPositiveButton(android.R.string.ok) {_,newColor,_ -> selectedColor = newColor}
                    .setNegativeButton(android.R.string.cancel) {_,_ -> Unit}
                    .showAlphaSlider(colorPreference.showAlphaSlider)
                    .showColorEdit(true)
                    .build()
                    .apply {
                        // set the color back to original color when touch outside dialog
                        setOnDismissListener {setColor(selectedColor)}

                        // don't popup keyboard immediately
                        window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
                    }
                    .show()
        }
    }
}

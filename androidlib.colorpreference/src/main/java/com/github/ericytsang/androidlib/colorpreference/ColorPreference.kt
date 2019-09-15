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
import com.github.ericytsang.androidlib.core.getDrawableCompat
import com.github.ericytsang.lib.noopclose.NoopClose
import com.github.ericytsang.lib.optional.Opt
import com.github.ericytsang.lib.prop.RaiiProp
import com.github.ericytsang.lib.prop.listen
import com.github.ericytsang.lib.prop.value
import java.io.Closeable

// todo: add licenses to the app
class ColorPreference(
        context:Context,
        attrs:AttributeSet)
    :Preference(context,attrs)
{
    private val attached = RaiiProp(Opt.none<Attached>())
    private val persistenceStrategy = RaiiProp(Opt.none<NoopClose<PersistenceStrategy>>())
    private val running = RaiiProp(Opt.none<Running>())

    private val runningAssigner = listOf(persistenceStrategy,attached).listen()
    {
        val attached = attached.value.invoke().opt
        val persistenceStrategy = persistenceStrategy.value.invoke().opt?.wrapee
        if (attached != null && persistenceStrategy != null)
        {
            running.value = {Opt.some(Running(attached,persistenceStrategy))}
        }
        else
        {
            running.value = {Opt.none()}
        }
    }

    private val typedArray = context.theme.obtainStyledAttributes(attrs,R.styleable.ColorPreference,0,0)
    private val showAlphaSlider = typedArray.getBoolean(R.styleable.ColorPreference_showAlphaSlider,true)

    init
    {
        typedArray.recycle()
        widgetLayoutResource = R.layout.layout__swatch
    }

    override fun onBindViewHolder(holder:PreferenceViewHolder)
    {
        super.onBindViewHolder(holder)
        attached.value = {Opt.some(Attached(this,holder))}
    }

    override fun onDetached()
    {
        attached.value = {Opt.none()}
        persistenceStrategy.value = {Opt.none()}
        running.value = {Opt.none()}
        runningAssigner.close()
        super.onDetached()
    }

    override fun onClick()
    {
        super.onClick()
        running.value.invoke().opt?.onClick()
    }

    fun setPersistenceStrategy(newPersistenceStrategy:PersistenceStrategy)
    {
        persistenceStrategy.value = {Opt.some(NoopClose(newPersistenceStrategy))}
    }

    interface PersistenceStrategy
    {
        fun save(context:Context,color:Int)
        fun load(context:Context):Int
    }

    private class Attached(
            val colorPreference:ColorPreference,
            holder:PreferenceViewHolder)
        :Closeable
    {
        val previewView = holder.itemView.findViewById<View>(R.id.color_sample)!!
        val checkeredBackgroundView = holder.itemView.findViewById<View>(R.id.checkered_background)!!
        init
        {
            checkeredBackgroundView.background = TilingDrawable(
                    colorPreference.context.getDrawableCompat(R.drawable.checkers)!!,
                    TilingDrawable.RepeatMode.FROM_CENTER)
        }
        override fun close() = Unit
        fun setPreviewColor(newColor:Int)
        {
            val background = previewView.background as GradientDrawable
            background.setColor(newColor)
        }
    }

    private class Running(
            val attached:Attached,
            val persistenceStrategy:PersistenceStrategy)
        :Closeable
    {
        init
        {
            val color = persistenceStrategy.load(attached.colorPreference.context)
            attached.setPreviewColor(color)
        }

        override fun close() = Unit

        fun onClick()
        {
            var selectedColor = persistenceStrategy.load(attached.colorPreference.context)
            val setColor = fun(newColor:Int)
            {
                persistenceStrategy.save(attached.colorPreference.context,newColor)
                attached.setPreviewColor(newColor)
            }
            ColorPickerDialogBuilder
                    .with(attached.colorPreference.context)
                    .setTitle(attached.colorPreference.title.toString())
                    .initialColor(selectedColor)
                    .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                    .density(12)
                    .setOnColorSelectedListener {setColor(it)}
                    .setOnColorChangedListener {setColor(it)}
                    .setPositiveButton(android.R.string.ok) {_,newColor,_ -> selectedColor = newColor}
                    .setNegativeButton(android.R.string.cancel) {_,_->Unit}
                    .showAlphaSlider(attached.colorPreference.showAlphaSlider)
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

package com.github.ericytsang.androidlib.core.viewholder

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.LayoutTransition
import android.animation.ValueAnimator
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.recyclerview.widget.RecyclerView
import com.github.ericytsang.androidlib.core.descendants
import com.github.ericytsang.androidlib.core.view.LayoutParamsGetter
import com.github.ericytsang.androidlib.core.view.LayoutParamsSetter
import com.github.ericytsang.androidlib.core.view.ViewDimensions
import com.github.ericytsang.androidlib.core.view.ViewDimensionsGetter
import com.github.ericytsang.androidlib.core.view.ViewProperty

abstract class ViewHolder<Model:Any>(
        val view:View)
    :RecyclerView.ViewHolder(
        view)
{
    private val originalLayoutParams = ViewGroup.LayoutParams(view.layoutParams)

    private val layoutTransitionTypes = setOf(
            LayoutTransition.APPEARING,
            LayoutTransition.CHANGE_APPEARING,
            LayoutTransition.CHANGE_DISAPPEARING,
            LayoutTransition.CHANGING,
            LayoutTransition.DISAPPEARING)

    /**
     * the [Model] that is currently being presented by this [ViewHolder].
     */
    var model:Model? = null
        private set

    /**
     * sets the [ViewHolder]'s [model], and updates the [ViewHolder]'s
     * [view] to present the passed [newModel].
     *
     * @param newModel reference to the new newModel to present.
     */
    fun set_model(newModel:Model)
    {
        val oldModel = model
        model = newModel

        // re-enable transition types to show animations // todo: depends on assuming that all transition types should be enabled...
        view.descendants
                .plus(view)
                .mapNotNull {it as? ViewGroup}
                .mapNotNull {it.layoutTransition}
                .forEach {lt -> layoutTransitionTypes.forEach {lt.enableTransitionType(it)}}

        onSetModel(oldModel,newModel)

        // adjust and animate root view dimensions
        if (oldModel != null && haveTheSameId(oldModel,newModel))
        {
            // get original dimensions
            val oldDimensions = ViewDimensions(
                    view.measuredWidth,
                    view.measuredHeight)

            // update dimensions
            view.measure(
                    fromLayoutParameterToMeasureSpec(originalLayoutParams.width,oldDimensions.width),
                    fromLayoutParameterToMeasureSpec(originalLayoutParams.height,oldDimensions.height))

            // get current dimensions
            val newDimensions = ViewDimensions(
                    view.measuredWidth,
                    view.measuredHeight)

            // set container to the old dimension to hide weird effects from call to measure
            view.layoutParams = ViewGroup.LayoutParams(originalLayoutParams)
            view.requestLayout()

            // animate view if it's getting smaller
            animateShrinkingIfNeeded(oldDimensions,newDimensions,originalLayoutParams,view,ViewProperty.Width())
            animateShrinkingIfNeeded(oldDimensions,newDimensions,originalLayoutParams,view,ViewProperty.Height())
        }

        // skip animations if displaying semantically different item // todo: depends on assuming that all transition types should be enabled...
        else
        {
            val allViews = view.descendants
            allViews.forEach {it.clearAnimation()}
            allViews
                    .plus(view)
                    .mapNotNull {it as? ViewGroup}
                    .mapNotNull {it.layoutTransition}
                    .forEach {lt -> layoutTransitionTypes.forEach {lt.disableTransitionType(it)}}
        }
    }

    /**
     * sets the [ViewHolder]'s [model], and updates the instance's [view] to
     * present the [model].
     *
     * @param model reference to the new model to present.
     */
    protected abstract fun onSetModel(oldModel:Model?,newModel:Model)

    /**
     * @returns true when the two parameters represent the same item
     * (e.g. they have the same id); false otherwise.
     */
    protected abstract fun haveTheSameId(oldModel:Model,newModel:Model):Boolean

    /**
     * the application context.
     */
    protected val context:Context = view.context

    private fun animateShrinkingIfNeeded(
            oldDimensions:ViewDimensions,
            newDimensions:ViewDimensions,
            originalLayoutParam:ViewGroup.LayoutParams,
            view:View,
            viewProperty:ViewProperty)
    {
        val oldDimension = viewProperty.accept(ViewDimensionsGetter(oldDimensions))
        val newDimension = viewProperty.accept(ViewDimensionsGetter(newDimensions))
        val originalLayoutParamValue = viewProperty.accept(LayoutParamsGetter(ViewGroup.LayoutParams(originalLayoutParam)))

        // if a current dimension is smaller
        if (newDimension < oldDimension)
        {
            // add a value animator to shrink the view's dimension
            ValueAnimator.ofInt(oldDimension,newDimension)
                    .apply {
                        addUpdateListener {
                            viewProperty.accept(LayoutParamsSetter(
                                    view.layoutParams,
                                    it.animatedValue as Int))
                            view.requestLayout()
                        }
                        addListener(object:AnimatorListenerAdapter()
                        {
                            override fun onAnimationEnd(animation:Animator?)
                            {
                                viewProperty.accept(LayoutParamsSetter(
                                        view.layoutParams,
                                        originalLayoutParamValue))
                                view.requestLayout()
                            }

                            override fun onAnimationCancel(animation:Animator?)
                            {
                                onAnimationEnd(animation)
                            }
                        })
                        interpolator = AccelerateDecelerateInterpolator()
                    }
                    .start()
        }
    }

    private fun fromLayoutParameterToMeasureSpec(layoutParam:Int,currentDimension:Int):Int
    {
        return when(layoutParam)
        {
            ViewGroup.LayoutParams.WRAP_CONTENT ->
                View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED)
            ViewGroup.LayoutParams.MATCH_PARENT ->
                View.MeasureSpec.makeMeasureSpec(currentDimension,View.MeasureSpec.EXACTLY)
            else ->
                if (layoutParam >= 0)
                {
                    View.MeasureSpec.makeMeasureSpec(layoutParam,View.MeasureSpec.EXACTLY)
                }
                else
                {
                    throw RuntimeException("invalid layout param....")
                }
        }
    }
}

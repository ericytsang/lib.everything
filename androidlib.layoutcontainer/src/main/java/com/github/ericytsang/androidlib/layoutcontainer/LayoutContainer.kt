package com.github.ericytsang.androidlib.layoutcontainer

import android.view.ViewGroup
import com.github.ericytsang.androidlib.core.layoutInflater

class LayoutContainer(
        layoutResId:Int,
        parent:ViewGroup)
    :kotlinx.android.extensions.LayoutContainer
{
    override val containerView = parent.context.layoutInflater.inflate(layoutResId,parent,false)!!
}

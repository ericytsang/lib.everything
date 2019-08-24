package com.github.ericytsang.lib.game

import com.github.ericytsang.lib.prop.ReadOnlyProp
import java.util.Locale

interface PlatformContext
{
    val locale:ReadOnlyProp<Unit,Locale>
    fun debug(debugLog:()->String)
}
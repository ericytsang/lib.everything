package com.github.ericytsang.androidlib.screenmeasurer

import com.github.ericytsang.androidlib.core.RealScreenSize
import com.github.ericytsang.lib.xy.XyBounds

data class ScreenDimensions(
        val realScreenSize:RealScreenSize,
        val screenBoundsExcludingNavBar:XyBounds,
        val screenBoundsIncludingNavBar:XyBounds)

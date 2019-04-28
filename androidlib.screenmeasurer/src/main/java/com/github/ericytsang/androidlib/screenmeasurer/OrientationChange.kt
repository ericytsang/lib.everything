package com.github.ericytsang.androidlib.screenmeasurer

import com.github.ericytsang.androidlib.core.Orientation

data class OrientationChange(
        val orientation:Orientation,
        val dimensions:ScreenDimensions)

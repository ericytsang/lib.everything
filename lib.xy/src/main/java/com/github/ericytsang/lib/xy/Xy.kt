package com.github.ericytsang.lib.xy

import java.io.Serializable

data class Xy(
        val x:Float,
        val y:Float)
    :Serializable
{
    companion object
    {
        val ZERO = Xy(0f,0f)
    }

    init
    {
        require(x.isNaN().not())
        require(y.isNaN().not())
    }

    operator fun plus(other:Xy):Xy
    {
        return Xy(x+other.x,y+other.y)
    }

    operator fun minus(other:Xy):Xy
    {
        return Xy(x-other.x,y-other.y)
    }

    operator fun times(other:Float):Xy
    {
        return Xy(x*other,y*other)
    }

    operator fun div(other:Float):Xy
    {
        return Xy(x/other,y/other)
    }

    val squaredDistance by lazy {x*x+y*y}
}

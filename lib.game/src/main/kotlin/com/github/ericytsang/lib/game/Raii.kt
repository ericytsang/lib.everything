package com.github.ericytsang.lib.game

interface Raii<R>
{
    fun close():R
}
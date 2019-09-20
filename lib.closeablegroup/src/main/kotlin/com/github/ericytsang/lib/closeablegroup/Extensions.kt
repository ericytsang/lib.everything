package com.github.ericytsang.lib.closeablegroup

fun <T> T.asCloseable(close:T.()->Unit):CloseAdapter<T>
{
    return CloseAdapter(this,close)
}
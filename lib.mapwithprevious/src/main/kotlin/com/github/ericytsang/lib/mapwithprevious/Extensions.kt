package com.github.ericytsang.lib.mapwithprevious

fun <T:Any,R:Any> Sequence<T>.mapWithPrevious(block:(previous:R?,element:T)->R):Sequence<R>
{
    var previous:R? = null
    return map {
        val transformed = block(previous,it)
        previous = transformed
        transformed
    }
}

fun <T:Any,R:Any> Iterable<T>.mapWithPrevious(block:(previous:R?,element:T)->R):Iterable<R>
{
    var previous:R? = null
    return map {
        val transformed = block(previous,it)
        previous = transformed
        transformed
    }
}

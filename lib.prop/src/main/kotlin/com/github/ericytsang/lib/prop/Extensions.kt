package com.github.ericytsang.lib.prop

import java.io.Closeable

val <Value:Any> ReadOnlyProp<Unit,Value>.value:Value
    get() = get(Unit)

var <Value:Any> Prop<Unit,Value>.value:Value
    get() = get(Unit)
    set(value) { set(Unit,value) }

fun Iterable<ReadOnlyProp<*,*>>.listen(onChanged:(ReadOnlyProp.Change<*,*>)->Unit):Closeable
{
    val closeables = map {it.listen(onChanged)}
    return Closeable {closeables.forEach {it.close()}}
}

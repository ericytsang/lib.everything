package com.github.ericytsang.lib.prop

import com.github.ericytsang.lib.closeablegroup.CloseableGroup
import com.github.ericytsang.lib.optional.Opt
import java.io.Closeable

val <Value:Any> ReadOnlyProp<Unit,Value>.value:Value
    get() = get(Unit)

var <Value:Any> MutableProp<Unit,Value>.value:Value
    get() = get(Unit)
    set(value) { set(Unit,value) }

val <Value:Any> ReadOnlyProp<Unit,()->Opt<Value>>.nullableValue:Value?
    get() = get(Unit).invoke().opt

var <Value:Any> MutableProp<Unit,()->Opt<Value>>.mutableNullableValue:()->Value?
    get() = get(Unit)().opt.let {{it}}
    set(value) { set(Unit) {Opt.of(value())} }

fun <Value:Any> MutableProp<Unit,Value>.set(value:Value):Value = set(Unit,value)

fun <Value:Any> MutableProp<Unit,()->Opt<Value>>.set(value:()->Opt<Value>):Value? = set(Unit,value).invoke().opt

fun Iterable<ReadOnlyProp<*,*>>.listen(callOnChangedNow:Boolean = true,onChanged:(ReadOnlyProp<*,*>?)->Unit):Closeable
{
    if (callOnChangedNow)
    {
        onChanged(null)
    }
    val closeables = map {it.listen {source -> onChanged(source)}}
    return Closeable {closeables.forEach {it.close()}}
}

interface AggregatedProp<State:Any>:ReadOnlyProp<Unit,State>,Closeable

fun <State:Any> Iterable<ReadOnlyProp<*,*>>.aggregate(stateFactory:(ReadOnlyProp<*,*>?)->State):AggregatedProp<State>
{
    val dataProp = DataProp(stateFactory(null)).debounced()
    val closeables = CloseableGroup()
    closeables += listen(false)
    {
        dataProp.value = stateFactory(it)
    }
    return object:AggregatedProp<State>,ReadOnlyProp<Unit,State> by dataProp
    {
        override fun close() = closeables.close()
    }
}

data class SessionFactoryParams<State>(
        val state:State,
        val closeables:CloseableGroup = CloseableGroup())

fun <State:Any> ReadOnlyProp<Unit,State>.statefulListen(sessionFactory:(SessionFactoryParams<State>)->Unit):Closeable
{
    fun closeableFactory(value:State):Closeable
    {
        val params = SessionFactoryParams(value)
        sessionFactory(params)
        return params.closeables
    }
    val raiiProp = RaiiProp(Opt.of(closeableFactory(value)))
    val closeables = CloseableGroup()
    closeables += raiiProp
    closeables += listOf(this).listen(false)
    {
        raiiProp.mutableNullableValue = {closeableFactory(value)}
    }
    return closeables
}

fun <Context:Any,Value:Any> MutableProp<Context,Value>.debounced() = Debouncer(this)

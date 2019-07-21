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
    val closeables = map {it.listen {change -> onChanged(change.source)}}
    return Closeable {closeables.forEach {it.close()}}
}

fun <State:Any> Iterable<ReadOnlyProp<*,*>>.aggregate(closeables:CloseableGroup = CloseableGroup(),stateFactory:()->State):ReadOnlyProp<Unit,State>
{
    val dataProp = DataProp(stateFactory()).debounced()
    closeables += listen(false)
    {
        dataProp.value = stateFactory()
    }
    return dataProp
}

fun <State:Any> ReadOnlyProp<Unit,State>.component(closeables:CloseableGroup = CloseableGroup(),closeableFactory:(State)->Closeable?):Closeable
{
    val raiiProp = RaiiProp(Opt.of(closeableFactory(value)?:Closeable{}))
    closeables += raiiProp
    closeables += listOf(this).listen(false)
    {
        raiiProp.mutableNullableValue = {closeableFactory(value)?:Closeable{}}
    }
    return closeables
}

fun <Context:Any,Value:Any> ReadOnlyProp<Context,Value>.withContext(contextFactory:()->Context):ReadOnlyProp<Unit,Value>
{
    val oldProp = this
    return object:ReadOnlyProp<Unit,Value>
    {
        override fun get(context:Unit):Value
        {
            return oldProp.get(contextFactory())
        }

        override fun getChange():ReadOnlyProp.Change<Unit,Value> {
            return ReadOnlyProp.Change(this,Unit,oldProp.getChange().oldValue,oldProp.getChange().newValue)
        }

        override fun listen(context:Unit,onChanged:(ReadOnlyProp.Change<Unit,Value>)->Unit):Closeable
        {
            return oldProp.listen(contextFactory()) {onChanged(it.map(this,Unit))}
        }

        override fun listen(onChanged:(ReadOnlyProp.Change<Unit,Value>)->Unit):Closeable
        {
            return oldProp.listen {onChanged(it.map(this,Unit))}
        }
    }
}

fun <Context:Any,Value:Any> MutableProp<Context,Value>.withContext(contextFactory:()->Context):MutableProp<Unit,Value>
{
    val oldProp = this
    val readOnlyProp = this.let {it as ReadOnlyProp<Context,Value>}.withContext(contextFactory)
    return object:MutableProp<Unit,Value>,ReadOnlyProp<Unit,Value> by readOnlyProp
    {
        override fun set(context:Unit,value:Value):Value
        {
            return oldProp.set(contextFactory(),value)
        }
    }
}

fun <OldContext:Any,NewContext:Any,Value:Any> ReadOnlyProp.Change<OldContext,Value>.map(newProp:ReadOnlyProp<NewContext,Value>,newContext:NewContext):ReadOnlyProp.Change<NewContext,Value>
{
    return ReadOnlyProp.Change(newProp,newContext,oldValue,newValue)
}

fun <Context:Any,OldValue:Any,NewValue:Any> ReadOnlyProp<Context,OldValue>.map(transform:(OldValue)->NewValue):ReadOnlyProp<Context,NewValue>
{
    val oldProp = this
    return object:ReadOnlyProp<Context,NewValue>
    {
        override fun get(context:Context):NewValue
        {
            return oldProp.get(context).let(transform)
        }

        override fun getChange():ReadOnlyProp.Change<Context,NewValue> {
            return ReadOnlyProp.Change(this,oldProp.getChange().context,transform(oldProp.getChange().oldValue),transform(oldProp.getChange().newValue))
        }

        override fun listen(context:Context,onChanged:(ReadOnlyProp.Change<Context,NewValue>)->Unit):Closeable
        {
            return oldProp.listen(context) {onChanged(it.map(this,transform))}
        }

        override fun listen(onChanged:(ReadOnlyProp.Change<Context,NewValue>)->Unit):Closeable
        {
            return oldProp.listen {onChanged(it.map(this,transform))}
        }
    }
}

fun <Context:Any,OldValue:Any,NewValue:Any> ReadOnlyProp.Change<Context,OldValue>.map(newProp:ReadOnlyProp<Context,NewValue>,transform:(OldValue)->NewValue):ReadOnlyProp.Change<Context,NewValue>
{
    return ReadOnlyProp.Change(newProp,context,transform(oldValue),transform(newValue))
}

fun <Context:Any,Value:Any> MutableProp<Context,Value>.debounced() = Debouncer(this)

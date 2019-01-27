package com.github.ericytsang.lib.prop

import java.io.Closeable

val <Value:Any> ReadOnlyProp<Unit,Value>.value:Value
    get() = get(Unit)

val <Value:Any> ReadOnlyProp<Unit,Value>.nullableValue:Value?
    get() = getNullable(Unit)

var <Value:Any> MutableProp<Unit,Value>.value:Value
    get() = get(Unit)
    set(value) { set(Unit,value) }

fun Iterable<ReadOnlyProp<*,*>>.listen(onChanged:(ReadOnlyProp.Change<*,*>)->Unit):Closeable
{
    val closeables = map {it.listen(onChanged)}
    return Closeable {closeables.forEach {it.close()}}
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

        override fun getNullable(context:Unit):Value?
        {
            return oldProp.getNullable(contextFactory())
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
        override fun set(context:Unit,value:Value)
        {
            oldProp.set(contextFactory(),value)
        }
    }
}

fun <OldContext:Any,NewContext:Any,Value:Any> ReadOnlyProp.Change<OldContext,Value>.map(newProp:ReadOnlyProp<NewContext,Value>,newContext:NewContext):ReadOnlyProp.Change<NewContext,Value>
{
    return when(this)
    {
        is ReadOnlyProp.Change.Before -> ReadOnlyProp.Change.Before(newProp,newContext,oldValue,newValue)
        is ReadOnlyProp.Change.After -> ReadOnlyProp.Change.After(newProp,newContext,oldValue,newValue)
    }
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

        override fun getNullable(context:Context):NewValue?
        {
            return oldProp.getNullable(context)?.let(transform)
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
    return when(this)
    {
        is ReadOnlyProp.Change.Before -> ReadOnlyProp.Change.Before(newProp,context,transform(oldValue),transform(newValue))
        is ReadOnlyProp.Change.After -> ReadOnlyProp.Change.After(newProp,context,transform(oldValue),transform(newValue))
    }
}

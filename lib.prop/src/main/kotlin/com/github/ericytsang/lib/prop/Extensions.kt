package com.github.ericytsang.lib.prop

import java.io.Closeable

val <Value:Any> ReadOnlyProp<Unit,Value>.value:Value
    get() = get(Unit)

val <Value:Any> ReadOnlyProp<Unit,Value>.nullableValue:Value?
    get() = getNullable(Unit)

var <Value:Any> MutableProp<Unit,Unit,Value>.value:Value
    get() = get(Unit)
    set(value) { set(Unit,Unit,value) }

fun <Context:Any,Value:Any> MutableProp<Context,Unit,Value>.set(context:Context,value:Value)
{
    set(context,Unit,value)
}

fun Iterable<ReadOnlyProp<*,*>>.listen(onChanged:(ReadOnlyProp<*,*>?)->Unit):Closeable
{
    onChanged(null)
    val closeables = map {it.listen {change -> onChanged(change.source)}}
    return Closeable {closeables.forEach {it.close()}}
}

fun <Context:Any,Value:Any> ReadOnlyProp<Context,Value>.withReadContext(contextFactory:()->Context):ReadOnlyProp<Unit,Value>
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

fun <ReadContext:Any,WriteContext:Any,Value:Any> MutableProp<ReadContext,WriteContext,Value>.withReadContext(contextFactory:()->ReadContext):MutableProp<Unit,WriteContext,Value>
{
    val oldProp = this
    val readOnlyProp = this.let {it as ReadOnlyProp<ReadContext,Value>}.withReadContext(contextFactory)
    return object:MutableProp<Unit,WriteContext,Value>,ReadOnlyProp<Unit,Value> by readOnlyProp
    {
        override fun set(readContext:Unit,writeContext:WriteContext,value:Value)
        {
            oldProp.set(contextFactory(),writeContext,value)
        }
    }
}

fun <ReadContext:Any,WriteContext:Any,Value:Any> MutableProp<ReadContext,WriteContext,Value>.withWriteContext(writeContextFactory:()->WriteContext):MutableProp<ReadContext,Unit,Value>
{
    val oldProp = this
    return object:MutableProp<ReadContext,Unit,Value>,ReadOnlyProp<ReadContext,Value> by oldProp
    {
        override fun set(readContext:ReadContext,writeContext:Unit,value:Value)
        {
            oldProp.set(readContext,writeContextFactory(),value)
        }
    }
}

fun <Context:Any,Value:Any> MutableProp<Context,Context,Value>.withReadWriteContext(contextFactory:()->Context):MutableProp<Unit,Unit,Value>
{
    return this
            .withWriteContext(contextFactory)
            .withReadContext(contextFactory)
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

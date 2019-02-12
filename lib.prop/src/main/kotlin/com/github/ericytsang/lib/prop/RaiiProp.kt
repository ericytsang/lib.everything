package com.github.ericytsang.lib.prop

import com.github.ericytsang.lib.optional.Opt
import java.io.Closeable

class RaiiProp<Value:Closeable>(initialValue:Opt<Value>):MutableProp<Unit,()->Opt<Value>>,Closeable
{
    private val prop = DataProp(initialValue)

    override fun set(context:Unit,value:()->Opt<Value>)
    {
        // get reference to soon-to-be-previous value
        val previousValue = get(context)

        // set value to nothing (to notify listeners so that they can clean up if required)
        prop.set(context,Opt.none())

        // close the previous value
        previousValue.invoke().opt?.close()

        // fetch and set the value to the new given value
        prop.set(context,value.invoke())
    }

    override fun get(context:Unit):()->Opt<Value>
    {
        return prop.get(context).let {{it}}
    }

    override fun getChange():ReadOnlyProp.Change<Unit,()->Opt<Value>>
    {
        return prop.getChange().map(this) {{it}}
    }

    override fun listen(context:Unit,onChanged:(ReadOnlyProp.Change<Unit,()->Opt<Value>>)->Unit):Closeable
    {
        return prop.listen(context)
        {
            change->
            onChanged(ReadOnlyProp.Change(this,Unit,change.oldValue.let {{it}},change.newValue.let {{it}}))
        }
    }

    override fun listen(onChanged:(ReadOnlyProp.Change<Unit,()->Opt<Value>>)->Unit):Closeable
    {
        return prop.listen()
        {
            change->
            onChanged(ReadOnlyProp.Change(this,Unit,change.oldValue.let {{it}},change.newValue.let {{it}}))
        }
    }

    override fun close()
    {
        value = {Opt.none()}
    }
}

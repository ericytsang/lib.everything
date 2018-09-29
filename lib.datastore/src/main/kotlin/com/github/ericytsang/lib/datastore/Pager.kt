package com.github.ericytsang.lib.datastore

interface Pager<in Index:Comparable<Index>,out Item:Any>
{
    fun page(start:Index,order:Order,limit:Int):List<Item>

    enum class Order
    {
        ASC, DSC;

        companion object
        {
            fun <T> asReversedIfDescending(comparator:Comparator<T>,order:Order):Comparator<T>
            {
                return when (order)
                {
                    Pager.Order.ASC -> comparator
                    Pager.Order.DSC -> comparator.reversedCompat()
                }
            }
        }
    }
}

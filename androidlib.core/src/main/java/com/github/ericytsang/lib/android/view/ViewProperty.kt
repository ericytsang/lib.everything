package com.github.ericytsang.androidlib.view

sealed class ViewProperty
{
    companion object
    {
        val values = listOf(Width(),Height())
    }

    abstract fun <Return> accept(v:Visitor<Return>):Return

    class Width:ViewProperty()
    {
        override fun <Return> accept(v:Visitor<Return>):Return
        {
            return v.visit(this)
        }
    }
    class Height:ViewProperty()
    {
        override fun <Return> accept(v:Visitor<Return>):Return
        {
            return v.visit(this)
        }
    }

    interface Visitor<Return>
    {
        fun visit(receiver:Width):Return
        fun visit(receiver:Height):Return
    }
}
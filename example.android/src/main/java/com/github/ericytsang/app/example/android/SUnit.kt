package com.github.ericytsang.app.example.android

import java.io.Serializable

// todo: move to a library module
class SUnit:Serializable
{
    override fun hashCode() = SUnit::class.simpleName!!.hashCode()
    override fun equals(other:Any?) = other is SUnit
}
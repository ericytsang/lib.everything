package com.github.ericytsang.lib.domainobjects

import java.io.Serializable

class SUnit:Serializable
{
    override fun hashCode() = SUnit::class.simpleName!!.hashCode()
    override fun equals(other:Any?) = other is SUnit
}
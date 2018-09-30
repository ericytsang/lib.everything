package com.github.ericytsang.lib.domainobjects

import java.io.Serializable

interface DataObject:Serializable
{
    override fun hashCode():Int
    override fun equals(other:Any?):Boolean
    override fun toString():String
}

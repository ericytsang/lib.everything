package com.github.ericytsang.lib.domainobjects

import java.io.Serializable

sealed class Either<A,B>:Serializable
{
    data class A<A,B>(val a:A):Either<A,B>()
    data class B<A,B>(val b:B):Either<A,B>()
}

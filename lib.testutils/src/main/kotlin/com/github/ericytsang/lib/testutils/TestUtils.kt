package com.github.ericytsang.lib.testutils

import org.hamcrest.BaseMatcher
import org.hamcrest.Description
import org.mockito.Matchers

object TestUtils
{
    class ExpectedExceptionNotThrownException internal constructor():Exception()

    fun exceptionExpected(block:()->Unit):Exception
    {
        try
        {
            block()
            throw ExpectedExceptionNotThrownException()
        }
        catch (ex:ExpectedExceptionNotThrownException)
        {
            throw ex
        }
        catch (ex:Exception)
        {
            ex.printStackTrace(System.out)
            return ex
        }
    }

    fun <Param:Any?> paramThat(predicateTrueConditions:String? = null,predicate:(Param)->Boolean):Param
    {
        return Matchers.argThat(
            object:BaseMatcher<Param>()
            {
                override fun describeTo(description:Description)
                {
                    description.appendText(predicateTrueConditions ?: predicate.toString())
                }

                override fun matches(p0:Any?):Boolean
                {
                    @Suppress("UNCHECKED_CAST")
                    return predicate(p0 as Param)
                }
            }
        )
    }
}

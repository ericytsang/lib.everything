package com.github.ericytsang.lib.onlysetonce.test

import com.github.ericytsang.lib.onlysetonce.OnlySetOnce
import org.junit.Test

class OnlySetOnceTest
{
    private var onlysetonce:Boolean? by OnlySetOnce()

    @Test
    fun setTwice()
    {
        onlysetonce = true
        check(onlysetonce == true)
        try
        {
            onlysetonce = false
            throw AssertionError("was able to set twice")
        }
        catch (ex:AssertionError)
        {
            throw ex
        }
        catch (ex:Exception)
        {
            ex.printStackTrace(System.out)
        }
        check(onlysetonce == true)
    }

    @Test
    fun normalUsageTest()
    {
        check(onlysetonce == null)
        onlysetonce = true
        check(onlysetonce == true)
    }

    @Test
    fun setToNull()
    {
        try
        {
            onlysetonce = null
            throw AssertionError("was able to set to null")
        }
        catch (ex:AssertionError)
        {
            throw ex
        }
        catch (ex:Exception)
        {
            ex.printStackTrace(System.out)
        }
    }
}

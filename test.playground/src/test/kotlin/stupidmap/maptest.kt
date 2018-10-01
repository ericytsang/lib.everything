package stupidmap

import org.junit.Test
import kotlin.test.assertEquals

class maptest
{
    @Test
    fun putIfAbsent()
    {
        val map = mutableMapOf<String,String>()
        assertEquals(null,map.putIfAbsent("hi","hi"))
    }

    @Test
    fun putIfAbsentOnExistingValue()
    {
        val map = mutableMapOf("hi" to "bye")
        assertEquals("bye",map.putIfAbsent("hi","hi"))
    }

    @Test
    fun computeIfAbsent()
    {
        val map = mutableMapOf<String,String>()
        assertEquals("hi",map.computeIfAbsent("hi") {"hi"})
    }

    @Test
    fun computeIfAbsentOnExistingValue()
    {
        val map = mutableMapOf("hi" to "bye")
        assertEquals("bye",map.computeIfAbsent("hi") {"hi"})
    }
}

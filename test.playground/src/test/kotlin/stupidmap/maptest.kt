package stupidmap

import org.junit.Test
import kotlin.test.assertEquals

class maptest
{
    @Test
    fun put_if_absent()
    {
        val map = mutableMapOf<String,String>()
        assertEquals(null,map.putIfAbsent("hi","hi"))
    }

    @Test
    fun put_if_absent_on_existing_value()
    {
        val map = mutableMapOf("hi" to "bye")
        assertEquals("bye",map.putIfAbsent("hi","hi"))
    }

    @Test
    fun compute_if_absent()
    {
        val map = mutableMapOf<String,String>()
        assertEquals("hi",map.computeIfAbsent("hi") {"hi"})
    }

    @Test
    fun compute_if_absent_on_existing_value()
    {
        val map = mutableMapOf("hi" to "bye")
        assertEquals("bye",map.computeIfAbsent("hi") {"hi"})
    }
}

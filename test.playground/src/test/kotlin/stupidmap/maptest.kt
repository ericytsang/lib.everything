package stupidmap

import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
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

    @Test
    fun serialize_test()
    {
        val a = A("HELLOOooooThereeee")
        val b = B("adasdasdasd")
        val byteOs = ByteArrayOutputStream()
        val objOs = ObjectOutputStream(byteOs)
        objOs.writeObject(a)
        objOs.writeObject(b)
        objOs.writeObject(a)
        objOs.writeObject(b)
        val byteIs = ByteArrayInputStream(byteOs.toByteArray())
        val objIs = ObjectInputStream(byteIs)
        println(objIs.readObject())
        println(objIs.readObject())
        println(objIs.readObject())
        println(objIs.readObject())
    }

    data class A(val string:String):Serializable
    {
        companion object
        {
            @JvmStatic
            private val serialVersionUID = 62784392L
        }
    }
    data class B(val string:String):Serializable
    {
        companion object
        {
            @JvmStatic
            private val serialVersionUID = 62784392L
        }
    }
}

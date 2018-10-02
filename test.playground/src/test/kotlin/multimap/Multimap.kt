package multimap

import com.google.common.collect.ArrayListMultimap
import org.junit.Test
import kotlin.test.assertEquals

class MultimapTest
{
    @Test
    fun what_does_get_return()
    {
        val multimap = ArrayListMultimap.create<String,Double>()
        assertEquals(emptyList<Double>(),multimap.get("hehhs"))
    }
}
package multimap

import com.google.common.collect.ArrayListMultimap
import org.junit.Test
import kotlin.test.assertEquals

class MultimapTest
{
    @Test
    fun whatDoesGetReturn()
    {
        val multimap = ArrayListMultimap.create<String,Double>()
        assertEquals(emptyList<Double>(),multimap.get("hehhs"))
    }
}
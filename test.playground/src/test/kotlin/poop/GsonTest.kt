package poop

import com.google.gson.GsonBuilder
import com.google.gson.annotations.Expose
import org.junit.Test

class GsonTest
{
    private val gsonBuilder = GsonBuilder()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
        .excludeFieldsWithoutExposeAnnotation()
        .serializeNulls()
        .create()

    @Test
    fun toJsonString() {
        println(gsonBuilder.toJson(
            ExposedList("hello", listOf("a","b","b")))
        )
    }
}

private data class ExposedList(
    val hello: String,
    @Expose
    val array: List<String>
)

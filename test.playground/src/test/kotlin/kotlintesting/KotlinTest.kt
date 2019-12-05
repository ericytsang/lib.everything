package kotlintesting

import org.junit.Test

class KotlinTest {

    @Test
    fun `are classes equal`() {
        assert(1::class == 5::class)
    }

    @Test
    fun `are classes non equal despite references being the same`() {
        val a: Number = 1
        val b: Number = 1.5
        assert(a::class != b::class)
        assert(a::class == 7::class)
        assert(b::class == 0.5::class)
    }
}
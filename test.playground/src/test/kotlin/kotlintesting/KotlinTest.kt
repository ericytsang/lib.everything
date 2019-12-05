package kotlintesting

import org.junit.Test

class KotlinTest {

    @Test
    fun `are classes equal`() {
        assert(1::class == 5::class)
    }
}
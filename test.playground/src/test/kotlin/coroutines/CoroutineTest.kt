package coroutines

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.setMain
import org.junit.Ignore
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CoroutineTest
{
    @Test
    fun cancelled_co_routine_throws_on_await()
    {
        var function = {}
        val deferred = GlobalScope.async()
        {
            delay(1000)
            function = {throw Throwable("oh no, i was set!")}
            5
        }
        runBlocking()
        {
            deferred.cancelAndJoin()

            // should throw exception here because the co-routine was cancelled earlier
            assertFailsWith(CancellationException::class) {println(deferred.await())}
            function()
        }
    }

    @Test
    fun completed_co_routine_returns_deferred_result()
    {
        val deferred = GlobalScope.async()
        {
            5
        }
        runBlocking()
        {
            deferred.await()
            deferred.cancelAndJoin()
            assertEquals(5,deferred.await())
        }
    }
}
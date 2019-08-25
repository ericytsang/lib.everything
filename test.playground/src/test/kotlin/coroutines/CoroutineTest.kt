package coroutines

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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

    @Test
    fun cancellation_does_not_run_code_after_delay_even_on_immediate_dispatcher()
    {
        var asyncThread:Thread? = null
        var function = {}
        val deferred = GlobalScope.async(Dispatchers.Main.immediate)
        {
            asyncThread = Thread.currentThread()
            delay(1000)
            function = {throw Throwable("oh no, i was set!")}
            5
        }
        runBlocking(Dispatchers.Main.immediate)
        {
            deferred.cancelAndJoin()

            // should throw exception here because the co-routine was cancelled earlier
            assertFailsWith(CancellationException::class) {println(deferred.await())}
            function()
            assertEquals(asyncThread,Thread.currentThread())
        }
    }
}
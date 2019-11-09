package coroutines

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.junit.Test
import java.lang.System.*
import java.lang.Thread.currentThread
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

    private suspend fun waitForASecond()
    {
        val endTime = currentTimeMillis()+1000
        while (currentTimeMillis() < endTime)
        {
            yield()
        }
        println("waited for 1 second")
    }

    private suspend fun networkRequest() = withContext(Dispatchers.IO)
    {
        logCurrentThreadName("4")
        repeat(5) {waitForASecond()}
        42
    }

    private fun logCurrentThreadName(label: String)
    {
        println("#$label ${currentThread().name}")
    }

    @Test
    fun letNetworkRequestFinish()
    {
        runBlocking()
        {
            logCurrentThreadName("1")
            launch()
            {
                val result = networkRequest()
                logCurrentThreadName("2")
                println("i got the answer!: $result")
            }
            logCurrentThreadName("3")
            println("hi, i'm not blocked lol")
        }
    }

    @Test
    fun cancelNetworkRequest()
    {
        runBlocking()
        {
            logCurrentThreadName("1")
            val coroutineScope = launch()
            {
                val result = networkRequest()
                logCurrentThreadName("2")
                println("i got the answer!: $result")
            }
            logCurrentThreadName("3")
            waitForASecond()
            coroutineScope.cancel()
            println("hi, i'm not blocked lol")
        }
    }
}
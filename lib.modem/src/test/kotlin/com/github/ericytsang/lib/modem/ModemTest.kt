package com.github.ericytsang.lib.modem

import com.github.ericytsang.lib.concurrent.awaitSuspended
import com.github.ericytsang.lib.concurrent.future
import com.github.ericytsang.lib.net.connection.Connection
import com.github.ericytsang.lib.net.host.TcpClient
import com.github.ericytsang.lib.net.host.TcpServer
import com.github.ericytsang.lib.testutils.TestUtils.assertAllWorkerThreadsDead
import com.github.ericytsang.lib.testutils.TestUtils.exceptionExpected
import org.hamcrest.CoreMatchers.`is`
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ErrorCollector
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.ConnectException
import java.net.InetAddress
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

class ModemTest
{
    companion object
    {
        const val TEST_PORT = 55652
    }
    @JvmField
    @Rule
    val errorCollector = ErrorCollector()
    val conn1:Connection
    val conn2:Connection
    init
    {
        val tcpClient = TcpClient.anySrcPort()
        val tcpServer = TcpServer(TEST_PORT)
        val q = LinkedBlockingQueue<Connection>()
        val t = thread()
        {
            q.put(tcpServer.accept())
        }
        conn1 = tcpClient.connect(TcpClient.Address(InetAddress.getByName("localhost"),TEST_PORT))
        conn2 = q.take()
        t.join()
        tcpServer.close()
    }

    @After
    fun teardown()
    {
        println("fun teardown() ===============================")
        conn1.close()
        conn2.close()
        assertAllWorkerThreadsDead()
    }

    @Test
    fun instantiate_test()
    {
        Modem.create(conn1)
        Modem.create(conn2)
    }

    @Test
    fun connect_accept_test()
    {
        val m1 = Modem.create(conn1)
        val m2 = Modem.create(conn2)
        val t1 = thread {m1.connect(Unit)}
        while (t1.state !in setOf(Thread.State.BLOCKED,Thread.State.WAITING,Thread.State.TIMED_WAITING));
        m2.accept()
        t1.join()
        m1.close()
        m2.close()
    }

    @Test
    fun concurrent_conversations_test_1()
    {
        val m1 = Modem.create(conn1)
        val m2 = Modem.create(conn2)
        val q = LinkedBlockingQueue<Connection>()
        thread {(1..5).forEach {q.put(m2.accept())}}
        val conn1s = (1..5).map {m1.connect(Unit)}
        val conn2s = (1..5).map {q.take()}
        // have 2 connections talk concurrently
        val threads = listOf(
            thread {(Byte.MIN_VALUE..Byte.MAX_VALUE).forEach {if (it%10 == 0) println(it);conn1s[0].outputStream.let(::DataOutputStream).writeInt(it)}},
            thread {(Byte.MIN_VALUE..Byte.MAX_VALUE).forEach {if (it%10 == 0) println(it);conn1s[1].outputStream.let(::DataOutputStream).writeInt(it)}},
            thread {(Byte.MIN_VALUE..Byte.MAX_VALUE).forEach {if (it%10 == 0) println(it);assert(conn2s[0].inputStream.let(::DataInputStream).readInt() == it)}},
            thread {(Byte.MIN_VALUE..Byte.MAX_VALUE).forEach {if (it%10 == 0) println(it);assert(conn2s[1].inputStream.let(::DataInputStream).readInt() == it)}})
        threads.forEach {it.join()}
        m1.close()
        m2.close()
    }

    @Test
    fun concurrent_conversations_test_2()
    {
        val m1 = Modem.create(conn1)
        val m2 = Modem.create(conn2)
        val q = LinkedBlockingQueue<Connection>()
        thread {(1..5).forEach {q.put(m2.accept())}}
        val conn1s = (1..5).map {m1.connect(Unit)}
        val conn2s = (1..5).map {q.take()}
        // have 1 connection send a lot, but corresponding one not receive
        val hangingThreads = listOf(
            thread {(Byte.MIN_VALUE..Byte.MAX_VALUE).forEach {if (it%10 == 0) println(it);conn1s[0].outputStream.let(::DataOutputStream).writeInt(it)}},
            thread {(Byte.MIN_VALUE..0).forEach {if (it%10 == 0) println(it);assert(conn2s[0].inputStream.let(::DataInputStream).readInt() == it)}})
        // have another connection send and receive as normal while the first one is blocked
        val joinableThreads = listOf(
            thread {(Byte.MIN_VALUE..Byte.MAX_VALUE).forEach {if (it%10 == 0) println(it);conn1s[1].outputStream.let(::DataOutputStream).writeInt(it)}},
            thread {(Byte.MIN_VALUE..Byte.MAX_VALUE).forEach {if (it%10 == 0) println(it);assert(conn2s[1].inputStream.let(::DataInputStream).readInt() == it)}})
        joinableThreads.forEach {it.join()}
        hangingThreads.forEach {it.join()}
        m1.close()
        m2.close()
        hangingThreads.forEach {it.stop()}
    }

    @Test
    fun closing_breaks_ongoing_connect()
    {
        val m1 = Modem.create(conn1)
        val m2 = Modem.create(conn2)
        val t = thread {
            val ex = exceptionExpected {
                m1.connect(Unit)
            }
            errorCollector.checkSucceeds {
                assert(ex is Modem.ModemException.ClosedException)
            }
        }
        t.awaitSuspended()
        m1.close()
        t.join()
        m1.close()
        m2.close()
    }

    @Test
    fun closing_breaks_ongoing_connections()
    {
        val m1 = Modem.create(conn1)
        val m2 = Modem.create(conn2)
        val q = LinkedBlockingQueue<Connection>()
        thread {(1..5).forEach {q.put(m2.accept())}}
        (1..5).map {m1.connect(Unit)}
        val conn2s = (1..5).map {q.take()}
        // have 2 connections talk concurrently
        val threads = conn2s.map {thread {it.inputStream.read()}}
        m1.close()
        threads.forEach {it.join()}
        m2.close()
    }

    @Test
    fun overflow_connects_get_refused()
    {
        val m1 = Modem.create(conn1,3)
        val m2 = Modem.create(conn2)
        val connectionRefusedCount = AtomicInteger(0)
        val doneLatch = CountDownLatch(1)
        val threads = (1..4).map {
            thread {
                errorCollector.checkSucceeds {
                    val ex = exceptionExpected {
                        m2.connect(Unit)
                    }
                    if (ex is ConnectException)
                    {
                        assert(connectionRefusedCount.getAndIncrement() == 0)
                        doneLatch.countDown()
                    }
                    else
                    {
                        assert(connectionRefusedCount.get() == 1)
                        assert(ex is Modem.ModemException.ClosedException)
                    }
                }
            }
        }
        doneLatch.await()
        m1.close()
        threads.forEach {it.join()}
        m2.close()
    }

    @Test
    fun close_underlying_connection_aborts_connect_1()
    {
        val m1 = Modem.create(conn1)
        val m2 = Modem.create(conn2)
        val thread = thread {
            errorCollector.checkSucceeds {
                exceptionExpected {
                    m2.connect(Unit)
                }
            }
        }
        thread.awaitSuspended()
        conn1.close()
        thread.join()
        m1.close()
        m2.close()
    }

    @Test
    fun close_underlying_connection_aborts_connect_2()
    {
        val m1 = Modem.create(conn1)
        val m2 = Modem.create(conn2)
        val thread = thread {
            errorCollector.checkSucceeds {
                exceptionExpected {
                    m2.connect(Unit)
                }
            }
        }
        thread.awaitSuspended()
        conn2.close()
        thread.join()
        m1.close()
        m2.close()
    }

    @Test
    fun close_underlying_connection_aborts_accept_1()
    {
        val m1 = Modem.create(conn1)
        val m2 = Modem.create(conn2)
        val thread = thread {
            errorCollector.checkSucceeds {
                exceptionExpected {
                    m2.accept()
                }
            }
        }
        thread.awaitSuspended()
        conn1.close()
        thread.join()
        m1.close()
        m2.close()
    }

    @Test
    fun close_underlying_connection_aborts_accept_2()
    {
        val m1 = Modem.create(conn1)
        val m2 = Modem.create(conn2)
        val thread = thread {
            errorCollector.checkSucceeds {
                exceptionExpected {
                    m2.accept()
                }
            }
        }
        thread.awaitSuspended()
        conn2.close()
        thread.join()
        m1.close()
        m2.close()
    }

    @Test
    fun closing_breaks_ongoing_read()
    {
        val m1 = Modem.create(conn1)
        val m2 = Modem.create(conn2)
        val t = thread {
            val connection = m2.accept()
            errorCollector.checkSucceeds {
                check(connection.inputStream.read() == -1)
            }
        }
        m1.connect(Unit)
        t.awaitSuspended()
        m1.close()
        t.join()
        m2.close()
    }

    @Test
    fun closing_underlying_connection_breaks_ongoing_read_1()
    {
        val m1 = Modem.create(conn1)
        val m2 = Modem.create(conn2)
        val t = thread {
            val connection = m2.accept()
            errorCollector.checkSucceeds {
                check(connection.inputStream.read() == -1)
            }
        }
        m1.connect(Unit)
        t.awaitSuspended()
        conn1.close()
        t.join()
        m2.close()
    }

    @Test
    fun closing_underlying_connection_breaks_ongoing_read_2()
    {
        val m1 = Modem.create(conn1)
        val m2 = Modem.create(conn2)
        val t = thread {
            val connection = m2.accept()
            errorCollector.checkSucceeds {
                check(connection.inputStream.read() == -1)
            }
        }
        m1.connect(Unit)
        t.awaitSuspended()
        conn2.close()
        t.join()
        m1.close()
        m2.close()
    }

    @Test
    fun closing_breaks_ongoing_accept()
    {
        val m1 = Modem.create(conn1)
        val m2 = Modem.create(conn2)
        val t = thread {
            errorCollector.checkSucceeds {
                exceptionExpected {
                    m1.accept()
                }
            }
        }
        t.awaitSuspended()
        m1.close()
        t.join()
        m2.close()
    }

    @Test
    fun half_close_test()
    {
        val m1 = Modem.create(conn1)
        val m2 = Modem.create(conn2)
        val q = LinkedBlockingQueue<Connection>()
        thread {(1..1).forEach {q.put(m2.accept())}}
        val conn1 = m1.connect(Unit)
        val conn2 = q.take()
        // have 2 connections talk concurrently
        conn1.outputStream.close()
        val threads = listOf(
            thread {(Byte.MIN_VALUE..Byte.MAX_VALUE).forEach {if (it%10 == 0) println(it);conn2.outputStream.let(::DataOutputStream).writeInt(it)}},
            thread {(Byte.MIN_VALUE..Byte.MAX_VALUE).forEach {if (it%10 == 0) println(it);assert(conn1.inputStream.let(::DataInputStream).readInt() == it)}})
        threads.forEach {it.join()}
        conn2.outputStream.close()
        m1.close()
        m2.close()
    }
}

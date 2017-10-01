package com.github.ericytsang.lib.modem;

import com.github.ericytsang.lib.net.connection.Connection;
import com.github.ericytsang.lib.net.host.TcpClient;
import com.github.ericytsang.lib.net.host.TcpServer;

import java.net.Socket;

/**
 * Created by surpl on 7/3/2017.
 */
public class ModemTest
{
    private final int TEST_PORT = 55652;
    private final Connection conn1;
    private final Connection conn2;

    private ModemTest()
    {
        TcpClient tcpClient = new TcpClient(()->new Socket());
        TcpServer tcpServer = TcpServer(TEST_PORT);
        val q = LinkedBlockingQueue<Connection>()
        kotlin.concurrent.thread()
        {
            q.put(tcpServer.accept())
        }
        conn1 = tcpClient.connect(TcpClient.Address(InetAddress.getByName("localhost"),TEST_PORT))
        conn2 = q.take()
        tcpServer.close()
    }

    @After
    fun teardown()
    {
        println("fun teardown() ===============================")
        Thread.sleep(100)
        conn1.close()
        conn2.close()
        TestUtils.assertAllWorkerThreadsDead(emptySet(),100)
        Thread.sleep(100)
    }

    @Test
    fun instantiateTest()
    {
        Modem(conn1)
        Modem(conn2)
    }

    @Test
    fun connectAcceptTest()
    {
        val m1 = Modem(conn1)
        val m2 = Modem(conn2)
        val t1 = thread {m1.connect(Unit)}
        Thread.sleep(10)
        assert(t1.isAlive)
            val t2 = thread {m2.accept()}
        Thread.sleep(10)
        assert(!t1.isAlive)
        assert(!t2.isAlive)
        t1.join()
        t2.join()
        m1.close()
        m2.close()
    }
}

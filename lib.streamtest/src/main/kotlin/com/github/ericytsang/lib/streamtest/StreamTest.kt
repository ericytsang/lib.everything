package com.github.ericytsang.lib.streamtest

import com.github.ericytsang.lib.concurrent.awaitSuspended
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ErrorCollector
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.OutputStream
import java.util.Arrays
import kotlin.concurrent.thread

/**
 * Created by surpl on 10/29/2016.
 */
abstract class StreamTest
{
    @JvmField
    @Rule
    val errorCollector = ErrorCollector()
    protected abstract val src:OutputStream
    protected abstract val sink:InputStream

    @Test
    fun pipe_byte_arrays()
    {
        val written = byteArrayOf(0,2,5,6)
        val read = byteArrayOf(0,0,0,0)
        val t = thread {
            errorCollector.checkSucceeds {
                DataInputStream(sink).readFully(read)
            }
        }
        src.write(written)
        src.close()
        t.join()
        assert(Arrays.equals(written,read))
    }

    @Test
    fun pipe_negative_number()
    {
        val t = thread {
            errorCollector.checkSucceeds {
                assert(sink.read() == 0xFF)
            }
        }
        src.write(-1)
        src.close()
        t.join()
    }

    @Test
    fun pipe_shorts()
    {
        val t = thread {
            errorCollector.checkSucceeds {
                assert(DataInputStream(sink).readShort() == 0.toShort())
                assert(DataInputStream(sink).readShort() == 1.toShort())
                assert(DataInputStream(sink).readShort() == (-1).toShort())
            }
        }
        DataOutputStream(src).writeShort(0)
        DataOutputStream(src).writeShort(1)
        DataOutputStream(src).writeShort(-1)
        t.join()
    }

    @Test
    fun pipe_string_objects()
    {
        val t = thread {
            errorCollector.checkSucceeds {
                assert(ObjectInputStream(sink).readObject() == "hello!!!")
            }
        }
        ObjectOutputStream(src).writeObject("hello!!!")
        src.close()
        t.join()
    }

    @Test
    fun pipe_multi_field_objects()
    {
        val t = thread {
            errorCollector.checkSucceeds {
                ObjectInputStream(sink).readObject()
            }
        }
        ObjectOutputStream(src).writeObject(RuntimeException("blehh"))
        src.close()
        t.join()
    }

    @Test
    fun long_string_read_first_then_write()
    {
        val string = "asdfm,bniuhdsbeubamdbiabdiauwemagianjkhfgbvdbvuidfgads" +
            "biavbajdfhgjadjvaihjxbcvuiasfbdgjaxcgviabfdgkjhsfgvuruibdfg,kbj" +
            "xufcergbjchbuvebkjhdbfguisdbkjhfbukyxghckjhbxkudfgybsdufbcjbvhs" +
            "uikdybgj,bgasdfm,bniuhdsbeubamdbiabdiauwemagianjkhfgbvdbvuidfga" +
            "dsbiavbajdfhgjadjvaihjxbcvuiasfbdgjaxcgviabfdgkjhsfgvuruibdfg,k" +
            "bjxufcergbjchbuvebkjhdbfguisdbkjhfbukyxghckjhbxkudfgybsdufbcjbv" +
            "hsuikdybgj,bgasdfm,bniuhdsbeubamdbiabdiauwemagianjkhfgbvdbvuidf" +
            "gadsbiavbajdfhgjadjvaihjxbcvuiasfbdgjaxcgviabfdgkjhsfgvuruibdfg" +
            ",kbjxufcergbjchbuvebkjhdbfguisdbkjhfbukyxghckjhbxkudfgybsdufbcj" +
            "bvhsuikdybgj,bgasdfm,bniuhdsbeubamdbiabdiauwemagianjkhfgbvdbvui" +
            "dfgadsbiavbajdfhgjadjvaihjxbcvuiasfbdgjaxcgviabfdgkjhsfgvuruibd" +
            "fg,kbjxufcergbjchbuvebkjhdbfguisdbkjhfbukyxghckjhbxkudfgybsdufb" +
            "cjbvhsuikdybgj,bgasdfm,bniuhdsbeubamdbiabdiauwemagianjkhfgbvdbv" +
            "uidfgadsbiavbajdfhgjadjvaihjxbcvuiasfbdgjaxcgviabfdgkjhsfgvurui" +
            "bdfg,kbjxufcergbjchbuvebkjhdbfguisdbkjhfbukyxghckjhbxkudfgybsdu" +
            "fbcjbvhsuikdybgj,bgasdfm,bniuhdsbeubamdbiabdiauwemagianjkhfgbvd" +
            "bvuidfgadsbiavbajdfhgjadjvaihjxbcvuiasfbdgjaxcgviabfdgkjhsfgvur" +
            "uibdfg,kbjxufcergbjchbuvebkjhdbfguisdbkjhfbukyxghckjhbxkudfgybs" +
            "dufbcjbvhsuikdybgj,bgasdfm,bniuhdsbeubamdbiabdiauwemagianjkhfgb" +
            "vdbvuidfgadsbiavbajdfhgjadjvaihjxbcvuiasfbdgjaxcgviabfdgkjhsfgv" +
            "uruibdfg,kbjxufcergbjchbuvebkjhdbfguisdbkjhfbukyxghckjhbxkudfgy" +
            "bsdufbcjbvhsuikdybgj,bg"
        val t = thread {
            errorCollector.checkSucceeds {
                assert(string == DataInputStream(sink).readUTF())
            }
        }
        t.awaitSuspended()
        DataOutputStream(src).writeUTF(string)
        t.join()
    }

    @Test
    fun simple_read_first_then_write_test()
    {
        val t = thread {
            errorCollector.checkSucceeds {
                assert(sink.read() == 234)
            }
        }
        t.awaitSuspended()
        src.write(234)
        t.join()
    }

    @Test
    fun complex_read_first_then_write_test()
    {
        val t = thread {
            errorCollector.checkSucceeds {
                ObjectInputStream(sink).readObject()
            }
        }
        t.awaitSuspended()
        ObjectOutputStream(src).writeObject(RuntimeException("blehh"))
        t.join()
    }

    @Test
    fun pipe_test_beyond_eof()
    {
        val t = thread {
            errorCollector.checkSucceeds {
                assert(sink.read() == 0)
                assert(sink.read() == 2)
                assert(sink.read() == 5)
                assert(sink.read() == 6)
                assert(sink.read() == 127)
                assert(sink.read() == 128)
                assert(sink.read() == 129)
                assert(sink.read() == 254)
                assert(sink.read() == 255)
                assert(sink.read() == -1)
                assert(sink.read() == -1)
                assert(sink.read() == -1)
                assert(sink.read() == -1)
            }
        }
        src.write(0)
        src.write(2)
        src.write(5)
        src.write(6)
        src.write(127)
        src.write(128)
        src.write(129)
        src.write(254)
        src.write(255)
        src.close()
        t.join()
    }

    @Test
    fun pipe_test_thread_interrupt()
    {
        src.write(0)
        src.write(2)
        src.write(5)
        src.write(6)
        src.write(127)
        src.write(128)
        src.write(129)
        src.write(254)
        src.write(255)
        assert(sink.read() == 0)
        assert(sink.read() == 2)
        assert(sink.read() == 5)
        assert(sink.read() == 6)
        assert(sink.read() == 127)
        assert(sink.read() == 128)
        assert(sink.read() == 129)
        assert(sink.read() == 254)
        assert(sink.read() == 255)
        val t = thread {
            errorCollector.checkSucceeds {
                assert(sink.read() == -1)
                assert(sink.read() == -1)
                assert(sink.read() == -1)
                assert(sink.read() == -1)
            }
        }
        t.awaitSuspended()
        src.close()
        src.close()
        src.close()
        t.join()
    }
}

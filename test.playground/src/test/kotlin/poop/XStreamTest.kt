package poop

import com.thoughtworks.xstream.XStream
import com.thoughtworks.xstream.annotations.XStreamAlias
import com.thoughtworks.xstream.converters.Converter
import com.thoughtworks.xstream.converters.MarshallingContext
import com.thoughtworks.xstream.converters.UnmarshallingContext
import com.thoughtworks.xstream.core.util.QuickWriter
import com.thoughtworks.xstream.io.HierarchicalStreamReader
import com.thoughtworks.xstream.io.HierarchicalStreamWriter
import com.thoughtworks.xstream.io.xml.CompactWriter
import com.thoughtworks.xstream.io.xml.XppDriver
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.Charset
import java.util.ArrayList



class XStreamTest
{

    private val m = Model("p&%oo")
    private val xStream = XStream()

    @Test
    fun marshallingTest1()
    {
        val bos = ByteArrayOutputStream()
        val h = XppDriver().createWriter(bos)
        xStream.marshal(m,h)
        val result = String(bos.toByteArray())
        println(result)
    }

    @Test
    fun marshallingTest2()
    {
        val bos = ByteArrayOutputStream()
        val h = RawXmlWriter(bos)
        xStream.marshal(m,h)
        val result = String(bos.toByteArray())
        println(result)
    }

    @Test
    fun marshallingTest3()
    {
        val result = xStream.toXML(m)
        println(result)
    }

    @Test
    fun marshallModels()
    {
        val teamBlog = Blog(Author("Guilherme Silveira"))
        teamBlog.entries.add(Entry("first","My first blog entry."))
        teamBlog.entries.add(Entry("tutorial",
                "Today we have developed a nice alias tutorial. Tell your friends! NOW!"))

        val xstream = XStream().apply()
        {
            registerConverter(object:Converter
            {
                override fun marshal(source:Any,writer:HierarchicalStreamWriter,context:MarshallingContext)
                {
                    writer.setValue(source.let {it as Author}.name)
                }

                override fun unmarshal(reader:HierarchicalStreamReader?,context:UnmarshallingContext?):Any
                {
                    throw UnsupportedOperationException("not implemented")
                }

                override fun canConvert(type:Class<*>?):Boolean
                {
                    return Author::class.java == type
                }
            })
        }
        println(xstream.toXML(teamBlog))
    }

    data class Model(
            @XStreamAlias("contents")
            val contents:String)
}

class RawXmlWriter(
        outputStream:OutputStream)
    :CompactWriter(OutputStreamWriter(outputStream,Charset.forName("UTF-8")))
{
    override fun writeAttributeValue(writer:QuickWriter,text:String)
    {
        writeRawXML(writer,text)
    }

    override fun writeText(writer:QuickWriter,text:String)
    {
        writeRawXML(writer,text)
    }

    protected fun writeRawXML(writer:QuickWriter,text:String)
    {
        writer.write(text)
    }
}

class Blog(private val writer:Author)
{
    val randomString = "helllooo from the other side~~~"
    val entries = ArrayList<Entry>()
}

class Author(val name:String)

class Entry(private val title:String,private val description:String)

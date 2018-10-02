import com.ctc.wstx.sax.WstxSAXParserFactory
import net.sf.joost.stx.Processor
import net.sf.joost.trax.TransformerImpl
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito.*
import org.mockito.runners.MockitoJUnitRunner
import org.xml.sax.InputSource
import java.io.ByteArrayInputStream
import javax.xml.transform.TransformerFactory
import javax.xml.transform.sax.SAXSource
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource

@RunWith(MockitoJUnitRunner::class)
class StxTest
{
    val styleSheetPath = "stylesheet.stx"
    val saxParserFactory = WstxSAXParserFactory()

    fun transformTest(inputString:String)
    {
        val sheetSource = StreamSource(javaClass.classLoader.getResourceAsStream(styleSheetPath))
        System.setProperty("javax.xml.transform.TransformerFactory","net.sf.joost.trax.TransformerFactoryImpl")
        val transformer = TransformerFactory
                .newInstance()
                .newTransformer(sheetSource)
                .let {it as TransformerImpl}
        val saxReader = saxParserFactory.newSAXParser().xmlReader
        val inputSource = InputSource(ByteArrayInputStream(inputString.toByteArray()))
        val saxSource = SAXSource(saxReader,inputSource)
        val result = StreamResult(System.out)
        val newTransformer = CustomTransformer(transformer.stxProcessor)
        println("input: $inputString")
        println()
        println("=-=-=-=-=-=-=-=-=-=-=-=-=-=")
        println()
        newTransformer.transform(saxSource,result)
    }

    @Test
    fun transform1()
    {
        transformTest("<root>&amp;</root>")
    }

    @Test
    fun transform2()
    {
        val mockk = mock(IStxExtensions::class.java)
        doReturn("<><><><>").`when`(mockk).unescapeHtml(Matchers.anyString())
        StxExtensions.delegate = mockk
        transformTest("<root>&amp;</root>")
    }

    @Test
    fun test_unescape_html()
    {
        println(StxExtensions.unescapeHtml("&amp;"))
    }

    class CustomTransformer(processor:Processor):TransformerImpl(processor)
}

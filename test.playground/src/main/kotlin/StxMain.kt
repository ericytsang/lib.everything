import com.ctc.wstx.sax.WstxSAXParserFactory
import net.sf.joost.stx.Processor
import net.sf.joost.trax.TransformerImpl
import org.xml.sax.InputSource
import javax.xml.transform.TransformerFactory
import javax.xml.transform.sax.SAXSource
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource

object StxMain
{
    val styleSheetPath = "stylesheet.stx"
    val saxParserFactory = WstxSAXParserFactory()

    @JvmStatic
    fun main(args:Array<String>)
    {
        val sheetSource = StreamSource(javaClass.classLoader.getResourceAsStream(styleSheetPath))
        System.setProperty("javax.xml.transform.TransformerFactory","net.sf.joost.trax.TransformerFactoryImpl")
        val transformer = TransformerFactory
                .newInstance()
                .newTransformer(sheetSource)
                .let {it as TransformerImpl}
        val saxReader = saxParserFactory.newSAXParser().xmlReader
        val inputSource = InputSource(System.`in`)
        val saxSource = SAXSource(saxReader,inputSource)
        val result = StreamResult(System.out)
        val newTransformer = CustomTransformer(transformer.stxProcessor)
        newTransformer.transform(saxSource,result)
    }

    class CustomTransformer(processor:Processor):TransformerImpl(processor)
}

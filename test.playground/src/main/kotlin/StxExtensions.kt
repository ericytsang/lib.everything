import org.apache.commons.lang3.StringEscapeUtils

class StxExtensions
{
    companion object:IStxExtensions
    {
        var delegate = object:IStxExtensions
        {
            override fun unescapeHtml(input:String):String
            {
                return StringEscapeUtils.unescapeHtml4(input)
            }

            override fun escapeHtml(input:String):String
            {
                return StringEscapeUtils.escapeHtml4(input)
            }
        }

        @JvmStatic
        override fun unescapeHtml(input:String):String
        {
            return delegate.unescapeHtml(input)
        }

        @JvmStatic
        override fun escapeHtml(input:String):String
        {
            return delegate.escapeHtml(input)
        }
    }
}

interface IStxExtensions
{
    fun unescapeHtml(input:String):String
    fun escapeHtml(input:String):String
}

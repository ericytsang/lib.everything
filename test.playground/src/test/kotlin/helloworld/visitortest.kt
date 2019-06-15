package helloworld

import org.junit.Test

class Test
{
    val hello = Hello().asUnionType()

    @Test
    fun testUnionTypes()
    {
        hello.accept(object:UnionType_Greeting.Visitor<Unit>
        {
            override fun visit(element:Hello) = println("override fun visit(element:Hello)")
            override fun visit(element:GoodBye) = println("override fun visit(element:GoodBye)")
        })
    }
}

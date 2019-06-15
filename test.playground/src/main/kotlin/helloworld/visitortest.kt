package helloworld

import com.github.ericytsang.generated.helloworld.UnionType_Greeting
import com.github.ericytsang.generated.helloworld.asUnionType
import com.github.ericytsang.lib.visitor.Visitable

@Visitable(Hello::class,GoodBye::class)
class Greeting

class Hello

class GoodBye

fun main()
{
    Hello().asUnionType().accept(object:UnionType_Greeting.Visitor<Unit>
    {
        override fun visit(element:Hello) = println("override fun visit(element:Hello)")
        override fun visit(element:GoodBye) = println("override fun visit(element:GoodBye)")
    })
}

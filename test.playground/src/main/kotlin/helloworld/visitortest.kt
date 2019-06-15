package helloworld

import com.github.ericytsang.lib.visitor.Visitable

@Visitable(Hello::class,GoodBye::class)
class Greeting

class Hello

class GoodBye

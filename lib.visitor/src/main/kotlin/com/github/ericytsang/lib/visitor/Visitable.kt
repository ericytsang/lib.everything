package com.github.ericytsang.lib.visitor

import kotlin.reflect.KClass

annotation class Visitable(vararg val classes:KClass<*>)

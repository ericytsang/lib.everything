package com.github.ericytsang.lib.property

object PropertyDebugMode {
    var debugMode = true
}

internal fun debugDo(block: () -> Unit) {
    if (PropertyDebugMode.debugMode) {
        block()
    }
}

internal fun debugPrintln(debugString: String) {
    if (PropertyDebugMode.debugMode) {
        println(debugString)
    }
}

internal fun debugPrintln(debugString: () -> String) {
    if (PropertyDebugMode.debugMode) {
        println(debugString())
    }
}
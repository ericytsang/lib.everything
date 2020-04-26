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
    debugDo {
        debugPrintln {debugString}
    }
}

internal fun debugPrintln(debugString: () -> String) {
    debugDo {
        Throwable(debugString()).printStackTrace(System.out)
    }
}
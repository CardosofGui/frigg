package com.br.frigg.logging

import android.util.Log

/**
 * Implementação Android do Logger usando android.util.Log.
 */
internal class AndroidLogger(private val tag: String) : Logger {
    override fun debug(message: () -> String) {
        Log.d(tag, message())
    }

    override fun info(message: () -> String) {
        Log.i(tag, message())
    }

    override fun warn(message: () -> String) {
        Log.w(tag, message())
    }

    override fun error(message: () -> String) {
        Log.e(tag, message())
    }

    override fun error(throwable: Throwable, message: () -> String) {
        Log.e(tag, message(), throwable)
    }
}

internal actual fun createLogger(name: String): Logger = AndroidLogger(name)

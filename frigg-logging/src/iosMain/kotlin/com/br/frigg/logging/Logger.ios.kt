package com.br.frigg.logging

import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
internal class IosLogger(private val tag: String) : Logger {
    private fun log(level: String, msg: String) {
        val formattedMessage = "[$level] [$tag] $msg"
        println(formattedMessage)
    }

    override fun debug(message: () -> String) {
        log("DEBUG", message())
    }

    override fun info(message: () -> String) {
        log("INFO", message())
    }

    override fun warn(message: () -> String) {
        log("WARN", message())
    }

    override fun error(message: () -> String) {
        log("ERROR", message())
    }

    override fun error(throwable: Throwable, message: () -> String) {
        val msg = message()
        val exceptionInfo = "\nException: ${throwable.message}\n${throwable.stackTraceToString()}"
        log("ERROR", "$msg$exceptionInfo")
    }
}

internal actual fun createLogger(name: String): Logger = IosLogger(name)

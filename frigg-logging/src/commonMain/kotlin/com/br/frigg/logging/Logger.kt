package com.br.frigg.logging

/**
 * Interface para logging multiplataforma.
 * Suporta logging com lazy evaluation através de lambdas.
 */
interface Logger {
    /**
     * Registra uma mensagem de debug.
     * @param message Função que retorna a mensagem a ser logada.
     */
    fun debug(message: () -> String)

    /**
     * Registra uma mensagem informativa.
     * @param message Função que retorna a mensagem a ser logada.
     */
    fun info(message: () -> String)

    /**
     * Registra uma mensagem de aviso.
     * @param message Função que retorna a mensagem a ser logada.
     */
    fun warn(message: () -> String)

    /**
     * Registra uma mensagem de erro.
     * @param message Função que retorna a mensagem a ser logada.
     */
    fun error(message: () -> String)

    /**
     * Registra uma mensagem de erro com exceção.
     * @param throwable Exceção associada ao erro.
     * @param message Função que retorna a mensagem a ser logada.
     */
    fun error(throwable: Throwable, message: () -> String)
}

/**
 * Função interna para criação de loggers multiplataforma.
 */
internal expect fun createLogger(name: String): Logger

/**
 * Objeto principal para obtenção de loggers multiplataforma.
 */
object FriggLogging {
    /**
     * Obtém uma instância de Logger para o nome especificado.
     * @param name Nome do logger, geralmente o nome da classe.
     * @return Instância de Logger configurada para a plataforma atual.
     */
    fun logger(name: String): Logger = createLogger(name)
}

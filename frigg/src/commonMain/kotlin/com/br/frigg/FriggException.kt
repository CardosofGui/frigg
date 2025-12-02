package com.br.frigg

/**
 * Classe base para todas as exceptions do módulo frigg.
 */
open class FriggException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Exception lançada quando não há espaço suficiente em disco para concluir a conversão.
 * @param availableSpace Espaço disponível em bytes.
 * @param requiredSpace Espaço necessário em bytes.
 */
open class StorageException(
    message: String,
    val availableSpace: Long,
    val requiredSpace: Long,
    cause: Throwable? = null
) : FriggException(message, cause)

/**
 * Exception lançada quando não há permissão para escrever no diretório de destino.
 * @param path Caminho do diretório ou arquivo onde a escrita falhou.
 */
open class WritePermissionException(
    message: String,
    val path: String,
    cause: Throwable? = null
) : FriggException(message, cause)

/**
 * Exception lançada quando o arquivo WAV é inválido, corrompido ou em formato não suportado.
 * @param filePath Caminho do arquivo que falhou na validação.
 * @param reason Motivo específico da falha de validação.
 */
open class InvalidFileException(
    message: String,
    val filePath: String,
    val reason: String,
    cause: Throwable? = null
) : FriggException(message, cause)

/**
 * Exception lançada quando o arquivo WAV não foi encontrado.
 * @param filePath Caminho do arquivo que não foi encontrado.
 */
open class FileNotFoundException(
    message: String,
    val filePath: String,
    cause: Throwable? = null
) : FriggException(message, cause)

/**
 * Exception lançada quando não há permissão para ler o arquivo WAV.
 * @param filePath Caminho do arquivo que não pode ser lido.
 */
open class ReadPermissionException(
    message: String,
    val filePath: String,
    cause: Throwable? = null
) : FriggException(message, cause)

/**
 * Exception lançada quando não é possível criar o diretório de saída.
 * @param directoryPath Caminho do diretório que não pôde ser criado.
 */
open class DirectoryCreationException(
    message: String,
    val directoryPath: String,
    cause: Throwable? = null
) : FriggException(message, cause)

/**
 * Exception lançada quando há erro ao carregar a biblioteca nativa.
 * @param libraryName Nome da biblioteca que falhou ao carregar, se disponível.
 */
open class NativeLibraryException(
    message: String,
    val libraryName: String? = null,
    cause: Throwable? = null
) : FriggException(message, cause)

/**
 * Exception lançada quando a conversão nativa falha.
 * @param wavPath Caminho do arquivo WAV de entrada.
 * @param mp3Path Caminho do arquivo MP3 de saída esperado.
 */
open class ConversionException(
    message: String,
    val wavPath: String,
    val mp3Path: String,
    cause: Throwable? = null
) : FriggException(message, cause)

/**
 * Exception lançada quando o arquivo MP3 foi criado mas está vazio ou não foi criado.
 * @param filePath Caminho do arquivo MP3 que está vazio ou não existe.
 */
open class EmptyFileException(
    message: String,
    val filePath: String,
    cause: Throwable? = null
) : FriggException(message, cause)

/**
 * Exception lançada para erros não mapeados.
 * @param cause Throwable original que causou o erro.
 */
open class UnknownFriggException(
    message: String,
    override val cause: Throwable
) : FriggException(message, cause)

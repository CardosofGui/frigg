# Verificação local

Verificado em: 2026-11-18

Comandos que funcionam de fato neste repositório, com o resultado esperado. O que está quebrado hoje fica em [traps.md](traps.md) com o contorno.

## Pré-requisitos

- JDK do SDKMAN e Android SDK/NDK 27 configurados em `local.properties`.

## Comandos verificados

| O que verifica | Comando | Resultado esperado |
| --- | --- | --- |
| Testes unitários JVM do Android | `./gradlew :frigg:testDebugUnitTest` | `BUILD SUCCESSFUL` (hoje só há o teste de exemplo) |

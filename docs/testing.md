# Verificação local

Verificado em: 2026-11-18

Comandos que funcionam de fato neste repositório, com o resultado esperado. O que está quebrado hoje fica em [traps.md](traps.md) com o contorno.

## Pré-requisitos

- JDK do SDKMAN; Android SDK em `local.properties` (`sdk.dir`); NDK `27.0.12077973` e CMake `3.22.1` (fixados em `frigg/build.gradle.kts`) instalados pelo SDK Manager.

## Comandos verificados

| O que verifica | Comando | Resultado esperado |
| --- | --- | --- |
| Compilação do código Android | `./gradlew :frigg:testDebugUnitTest` | `BUILD SUCCESSFUL` **sem executar testes**: o teste de exemplo está em `androidHostTest`, que este módulo não compila ([T-003](traps.md#t-003)) |

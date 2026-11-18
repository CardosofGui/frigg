# Armadilhas conhecidas

Armadilhas, falhas e incidentes que já aconteceram neste repositório. Leia as entradas da área antes de mexer nela. Ao corrigir um bug ou incidente que possa se repetir, registre a armadilha aqui no mesmo PR da correção.

| ID | Armadilha | Área | Severidade | Status |
| --- | --- | --- | --- | --- |
| [T-001](#t-001) | Bibliotecas nativas precisam de alinhamento de página de 16 KB | Build Android | média | mitigado |
| [T-002](#t-002) | Consumidor principal usa cópia internalizada, não a versão publicada | Consumidores | média | ativo |
| [T-003](#t-003) | Testes em `androidHostTest` nunca rodam | Testes | média | ativo |

## Build Android

### T-001

**Bibliotecas nativas precisam de alinhamento de página de 16 KB**

- **Status:** mitigado · **Severidade:** média · **Visto em:** 2026-04 · **Verificado em:** 2026-11-18
- **Sintoma:** Bibliotecas nativas incompatíveis com dispositivos Android 15+ que usam páginas de memória de 16 KB.
- **Causa raiz:** O `.so` era gerado com NDK antigo e sem a flag de alinhamento.
- **Como evitar:** Manter NDK 27 ou superior e a flag de linker `-Wl,-z,max-page-size=16384` em todo alvo nativo novo.
- **Como detectar:** `llvm-objdump -p <lib>.so | grep LOAD` deve mostrar `align 2**14` em todos os segmentos (ou o script `check_elf_alignment.sh` do Android); `zipalign -c -P 16 -v 4 <app>.apk` só complementa, checando o alinhamento dentro do APK.
- **Referências:** Commit `056c288`.

## Consumidores

### T-002

**Consumidor principal usa cópia internalizada, não a versão publicada**

- **Status:** ativo · **Severidade:** média · **Visto em:** 2026-06 · **Verificado em:** 2026-11-18
- **Sintoma:** Mudança publicada aqui não chega ao app Murb.
- **Causa raiz:** O `batalhafunk-android-v2` internalizou o conversor WAV→MP3 nos módulos `:shared-audio-converter` e `:audio-converter-native`, com pacotes renomeados, e não depende mais deste artefato.
- **Como evitar:** Antes de alterar a API ou o código nativo, confirmar quais consumidores ainda usam a versão publicada e levar a correção também à cópia do app quando for o caso.
- **Como detectar:** Procurar a dependência deste artefato nos catálogos Gradle dos consumidores.
- **Referências:** batalhafunk-android-v2 PR #436 (CRI-5).

## Testes

### T-003

**Testes em `androidHostTest` nunca rodam**

- **Status:** ativo · **Severidade:** média · **Visto em:** 2026-11 · **Verificado em:** 2026-11-18
- **Sintoma:** `./gradlew :frigg:testDebugUnitTest` termina com `BUILD SUCCESSFUL` sem compilar nem executar nenhum teste.
- **Causa raiz:** O módulo usa `com.android.library` com `androidTarget`, que só compila o source set `androidUnitTest`; o teste existente está em `frigg/src/androidHostTest`.
- **Como evitar:** Colocar testes novos em `androidUnitTest` (ou migrar o módulo para o plugin KMP de biblioteca Android) num PR de código; não tomar o `BUILD SUCCESSFUL` como prova de testes.
- **Como detectar:** Depois da task, `frigg/build/test-results` não existe e não há `compileDebugUnitTestKotlinAndroid` no log.
- **Referências:** `frigg/build.gradle.kts`; `frigg/src/androidHostTest/`.

## Como registrar

Cada entrada usa um ID `T-NNN` estável (nunca reutilizado) e os campos Status, Severidade, Visto em (mês da ocorrência), Verificado em (dia em que foi conferida no código atual), Sintoma, Causa raiz, Como evitar, Como detectar e Referências.

- Status: `ativo` (o risco existe hoje), `mitigado` (há guarda automática ou correção, e a entrada explica a guarda) ou `resolvido` (causa eliminada; remover a entrada, salvo se a lição ainda orienta trabalho futuro).
- Severidade: `alta` (dado, dinheiro, produção ou segurança), `média` (build, deploy, CI ou retrabalho relevante) ou `baixa` (perda de tempo).

Só entra o que já aconteceu, tem evidência verificável, não é óbvio pelo código e pode se repetir. Nunca registre segredos, valores de credenciais ou dados pessoais.

# Armadilhas conhecidas

Armadilhas, falhas e incidentes que já aconteceram neste repositório. Leia as entradas da área antes de mexer nela. Ao corrigir um bug ou incidente que possa se repetir, registre a armadilha aqui no mesmo PR da correção.

| ID | Armadilha | Área | Severidade | Status |
| --- | --- | --- | --- | --- |
| [T-001](#t-001) | Bibliotecas nativas precisam de alinhamento de página de 16 KB | Build Android | média | mitigado |
| [T-002](#t-002) | Consumidor principal usa cópia internalizada, não a versão publicada | Consumidores | média | ativo |

## Build Android

### T-001

**Bibliotecas nativas precisam de alinhamento de página de 16 KB**

- **Status:** mitigado · **Severidade:** média · **Visto em:** 2026-04
- **Sintoma:** Bibliotecas nativas incompatíveis com dispositivos Android 15+ que usam páginas de memória de 16 KB.
- **Causa raiz:** O `.so` era gerado com NDK antigo e sem a flag de alinhamento.
- **Como evitar:** Manter NDK 27 ou superior e a flag de linker `-Wl,-z,max-page-size=16384` em todo alvo nativo novo.
- **Como detectar:** Verificar o alinhamento dos `.so` no APK com `zipalign -c -P 16 -v 4` ou pelo aviso do Play Console.
- **Referências:** Commit `056c288`.

## Consumidores

### T-002

**Consumidor principal usa cópia internalizada, não a versão publicada**

- **Status:** ativo · **Severidade:** média · **Visto em:** 2026-07
- **Sintoma:** Mudança publicada aqui não chega ao app Murb.
- **Causa raiz:** O `batalhafunk-android-v2` internalizou o conversor WAV→MP3 nos módulos `:shared-audio-converter` e `:audio-converter-native`, com pacotes renomeados, e não depende mais deste artefato.
- **Como evitar:** Antes de alterar a API ou o código nativo, confirmar quais consumidores ainda usam a versão publicada e levar a correção também à cópia do app quando for o caso.
- **Como detectar:** Procurar a dependência deste artefato nos catálogos Gradle dos consumidores.
- **Referências:** batalhafunk-android-v2 PR #436 (CRI-5).

## Como registrar

Cada entrada usa um ID `T-NNN` estável (nunca reutilizado) e os campos Status (`ativo`, `mitigado` ou `resolvido`), Severidade (`alta`, `média` ou `baixa`), Visto em, Sintoma, Causa raiz, Como evitar, Como detectar e Referências. Só entra o que já aconteceu, tem evidência verificável, não é óbvio pelo código e pode se repetir. Nunca registre segredos, valores de credenciais ou dados pessoais.

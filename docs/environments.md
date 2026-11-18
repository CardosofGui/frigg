# Publicação e versões

Verificado em: 2026-11-18

A Frigg é uma biblioteca: não tem ambientes de execução, só publicação.

| Item | Valor |
| --- | --- |
| Artefato | `io.github.cardosofgui:frigg` no Maven Central |
| Plugin | `com.vanniktech.maven.publish` 0.34.0, com assinatura (`signing`) e `mavenCentralPublishing=true` |
| Versão no build | `frigg.version` em `gradle.properties` (hoje `1.1.2`) |
| Última versão no Maven Central | `1.1.2` (2026-04-06); não existe `1.2.0` publicado |
| Última release do GitHub | `1.2.0`, apontando para o commit `056c288`, cujo `frigg.version` é `1.1.2` |
| Versão citada no README | `1.1.0` |

As versões divergem. A referência é o Maven Central: a próxima publicação precisa ser maior que `1.1.2`, com `frigg.version`, tag e README iguais a ela. Não há workflow de CI; a publicação é manual, com as credenciais do Maven Central e a chave de assinatura no ambiente de quem publica, nunca no repositório.

O principal consumidor histórico, o app Murb, usa uma cópia internalizada em vez do artefato publicado ([T-002](traps.md#t-002)).

# Publicação e versões

Verificado em: 2026-11-18

A Frigg é uma biblioteca: não tem ambientes de execução, só publicação.

| Item | Valor |
| --- | --- |
| Artefato | `io.github.cardosofgui:frigg` no Maven Central |
| Plugin | `com.vanniktech.maven.publish` 0.34.0, com assinatura (`signing`) e `mavenCentralPublishing=true` |
| Versão no build | `frigg.version` em `gradle.properties` (hoje `1.1.2`) |
| Última release do GitHub | `1.2.0` (2026-04-06) |
| Versão citada no README | `1.1.0` |

As três versões divergem: antes de publicar, alinhar `frigg.version`, a tag da release e o README. Não há workflow de CI; a publicação é manual, com as credenciais do Maven Central e a chave de assinatura no ambiente de quem publica, nunca no repositório.

O principal consumidor histórico, o app Murb, usa uma cópia internalizada em vez do artefato publicado ([T-002](traps.md#t-002)).

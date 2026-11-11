# Plano de testes — Java 27 no poc-collector

## Escopo e pré-condições

- Executar a linha de base com Java 21 antes de alterar o POM; guardar saídas de versão, build, testes, portas e rotas em artefato temporário fora do Git.
- Executar a candidata com OpenJDK 27.0.x e Maven; o ambiente local observado para este plano é OpenJDK 27.0.1 (Homebrew) e Maven 3.10.0.
- Usar fixtures locais e transportes falsos. Nenhum teste do plano deve acessar Reddit, Google, WordPress ou outro serviço real, nem usar credenciais reais.
- Coordenar a instalação do artefato com `poc-collector-runner` e a cópia em `poc-dashboard/libs/poc-collector`. Os repositórios consumidores devem estar limpos ou ter suas alterações locais preservadas; o runner já possui alterações locais de documentação e não deve ser limpo pelo plano.
- A versão efetiva do artefato e o release de bytecode precisam ser conferidos no POM e no JAR produzido. O `pom.xml` atual ainda define release 21, portanto os critérios Java 27 estão pendentes até a implementação.

## Critérios e verificações

| Critério | Camada | Caso / comando | Estado |
|---|---|---|---|
| AC-01a — JDK, Maven e release confirmam Java 27 | Ambiente e build | Registrar `java -version`, `mvn -v`, `mvn help:evaluate -Dexpression=maven.compiler.release -q -DforceStdout`; conferir também a configuração efetiva do compiler plugin e o major version do JAR/classes. Repetir para cada imagem ou instalador incluído. | A criar na implementação; o ambiente local já tem JDK 27.0.1, mas o POM segue em 21. |
| AC-01b — instalação do coletor e consumidor | Integração de build | Sob Java 27, executar `mvn -B clean install` no coletor com repositório Maven temporário; instalar/selecionar esse artefato no runner e compilar/testar o runner. Construir o dashboard com seu snapshot local e verificar que a cópia em `libs/poc-collector` resolve a mesma versão candidata. | A criar; confirmar os comandos e coordenadas finais durante a implementação. |
| AC-01b — regressões de parser, JSON e OAuth | Unitária | `mvn -B -Dtest=FeedItemJsonTest,RssFeedParserTest,RedditOAuthAuthorizationTest,RedditOAuthTokenProviderTest,RedditCollectorTest test`, usando apenas fixtures e transportes falsos. | Existente; repetir nos dois JDKs. |
| AC-02 — feed malformado e parsing | Unitária | Executar `RssFeedParserTest` com feed inválido e XML com entidade externa; comparar aceitação/rejeição e erros com a linha de base Java 21. Adicionar fixture de XML truncado se a linha de base não cobrir esse formato. | Parcialmente existente (`rejectsNonFeedXml`, `rejectsExternalEntityPayload`); fixture de truncamento a confirmar/criar. |
| AC-02 — timeout de provedor | Unitária, sem rede | Injetar `FeedFetcher` falso que lança `FeedFetchException` de timeout; confirmar a mesma classe/categoria de falha e que nenhum conteúdo sensível aparece na mensagem. Comparar com Java 21. | A criar; não há teste determinístico de timeout no conjunto atual. |
| AC-02 — erro de OAuth | Unitária, sem rede | Usar resposta falsa HTTP 401 e corpo `fixture-denied`; verificar falha segura, ausência do token/corpo na mensagem e mesmo comportamento no baseline. | Existente em `RedditOAuthTokenProviderTest` e `RedditCollectorTest`; repetir nos dois JDKs. |
| AC-02 — consumidor não executa bytecode 27 em JVM 21 | Integração de build | Resolver o artefato instalado pelo runner em ambiente Java 21 e validar o comportamento esperado para o bytecode publicado. Se a exceção temporária de bytecode 21 for escolhida, conferir o `major version` 65 e executar o runner no JDK 21; após coordenar a migração do consumidor, repetir com Java 27. | A criar; decisão/exceção registrada no PR de implementação. |
| AC-03 — entradas e acesso sem ampliar exposição | Segurança / contrato | Repetir testes locais de DNS rebinding, faixas IP bloqueadas, XML/XXE, OAuth fail-closed e autenticação da API. Comparar `lsof -nP -iTCP -sTCP:LISTEN` antes e depois de compilar/executar os testes; confirmar que nenhum novo bind/porta fica ativo. Revisar diff e logs de teste para segredos. | Testes de URL, OAuth, parser e API existentes; comparação de portas e revisão de logs a criar no PR. |
| AC-04 — retorno para Java 21 | Manual exploratório | Em checkout temporário, restaurar o POM/runtime do baseline, executar `java -version`, `mvn -v`, `mvn -B clean install` e os testes focados; verificar que os contratos e arquivos JSON persistidos continuam legíveis. Não reverter nem alterar a branch de trabalho durante este teste. | A criar no PR; baseline identificado como `origin/develop` antes da migração. |

## Cenários negativos e de borda

1. Feed RSS truncado (`<rss><channel><item>`): deve falhar com a mesma categoria do Java 21, sem stack trace ou dados externos no payload público.
2. `FeedFetcher` falso que lança `new FeedFetchException("fixture timeout")`: deve falhar de modo determinístico, sem tentativa de rede real.
3. OAuth HTTP 401 com corpo de fixture contendo texto identificável: erro não deve expor token, segredo ou corpo da resposta.
4. Host de feed que resolve primeiro para `8.8.8.8` e depois `10.0.0.5`: a coleta deve continuar bloqueada após a migração.
5. Executar uma classe compilada com release 27 sob JVM 21: a falha de versão deve ser detectada no gate, ou evitada pela exceção de bytecode 21 documentada.
6. JDK ou Maven divergente do alvo: o plano deve falhar cedo ao verificar `java -version` e `mvn -v`, sem considerar apenas `--release` como prova do runtime.
7. Repositório Maven temporário vazio: instalação e resolução do runner devem provar que o consumidor encontra o artefato candidato, sem depender de um snapshot antigo no cache do usuário.

## Roteiro exploratório

1. Em checkout temporário do baseline Java 21, registrar `git rev-parse HEAD`, `java -version`, `mvn -v`, release efetivo, portas em escuta e versão do JAR.
2. Executar `mvn -B clean install` e os testes focados listados acima; arquivar apenas saídas sanitizadas e os relatórios Surefire.
3. Repetir os passos 1 e 2 na candidata Java 27, apontando Maven para um repositório local temporário e registrando o patch/distribuição usados.
4. Instalar a candidata no repositório Maven temporário; compilar/testar o runner contra esse artefato e verificar a versão resolvida com `mvn dependency:tree`.
5. Atualizar/validar o snapshot do coletor no dashboard e executar `mvn -B clean test`; quando o perfil `postgres-it` fizer parte do gate, executar com Docker disponível e registrar os resultados separados.
6. Comparar os relatórios do baseline e da candidata; investigar qualquer diferença em parsing, JSON, TLS, OAuth, portas, rotas ou status de erro antes de aceitar.
7. Fazer o teste de retorno em checkout temporário usando o commit baseline registrado no início, confirmar build Java 21 e leitura dos mesmos formatos de dados, e descartar o checkout temporário sem alterar as branches de trabalho.
8. Revisar logs, diffs e relatórios para garantir que não contêm tokens, URLs secretas ou dados pessoais reais.

## Dados de teste

- Fixtures existentes: `src/test/resources/feeds/google-news.xml` e `src/test/resources/feeds/wordpress-atom.xml`.
- Feed truncado: `<rss><channel><item>`.
- Timeout: `FeedFetcher` falso que lança `FeedFetchException("fixture timeout")`.
- Erro OAuth: resposta falsa `status=401`, corpo `fixture-denied`, sem credenciais reais.
- DNS rebinding: primeira resolução `8.8.8.8`; segunda resolução `10.0.0.5`.
- Dados de autenticação: valores sintéticos usados apenas pelos testes existentes; não copiar valores de ambiente local para arquivos ou logs.

## Fora do plano

- Chamadas ao vivo para fontes externas, aprovação da Data API do Reddit, deploy ou publicação de artefatos.
- Migração de dados, alterações funcionais do serviço ou aceitação de APIs preview.
- Atualizar runner/dashboard como parte desta execução do plano; eles são consumidores a validar na fase de implementação coordenada.

## Evidência da implementação (11/11/2026)

Atendido em parte: JDK 27.0.1 / Maven 3.9.16; `clean install` passou com 43 testes (0 falhas/erros). O JAR segue em bytecode 21 pela exceção de compatibilidade da spec; runner e dashboard candidatos compilaram contra ele. Próximo gate: Docker/PostgreSQL e comparação de rollback/portas.

# SDD Index

The **Status column is the state machine of the cycle** — prompts read it to decide what runs, and `/implement-spec` refuses specs that aren't `Approved`. Keep it accurate.

| Status | Meaning | Who sets it |
|---|---|---|
| `Draft` | Spec written, not approved — may still change | `/new-spec` · `/spec-from-issue` |
| `Approved` | **Gate 1 passed — the human approval act IS changing this cell** (edit it yourself, or tell the agent "approved" and it updates + commits) | **Human only** |
| `In progress` | Being implemented | `/implement-spec` (on start) |
| `Shipped` | MR/PR open, waiting CI + human review (Gate 2) | `/open-mr` |
| `Done` | Merged | `/next` (on detecting the merge) or human |
| `Blocked` | Waiting on something external — say what in Notes | Anyone (state the reason) |

Rules: agents NEVER set `Approved`. A spec that needs rework after approval goes back to `Draft` (approval is re-required). Every spec directory must have a row here — an unlisted spec doesn't exist for the cycle.

| Spec | Issue | Status | Notes |
|------|-------|--------|-------|
| [`20261106-multi-source-collector-mini/`](specs/20261106-multi-source-collector-mini/spec.md) | — | Done | Spec histórica superada pelo contexto canônico privado; Mercado Livre removido; conectores e gates atuais documentados no ai-init-internal |
| [`20261106-renomeacao-collector-mini/`](specs/20261106-renomeacao-collector-mini/spec.md) | — | Done | Renomeação para poc-collector e pacote com.henrique.collector |
| [`20261106-versoes-maven-em-properties/`](specs/20261106-versoes-maven-em-properties/spec.md) | — | Done | Versões de dependências centralizadas em properties |
| [`20261106-acesso-a-reddit-data-api/`](specs/20261106-acesso-a-reddit-data-api/spec.md) | — | Blocked | Spec aprovada pelo usuário em 2026-11-11; adaptador oficial fail-closed implementado. Acesso ao vivo aguarda aprovação explícita da Reddit e credenciais runtime; sem scraping |
| [`20261106-rota-oficial-issuu/`](specs/20261106-rota-oficial-issuu/spec.md) | — | Done | Pesquisa concluída: API oficial usa token para operações de conta; sem busca/feed público documentado, então fonte permanece desativada |
| [`20261106-persistencia-agendamento-e-retencao/`](specs/20261106-persistencia-agendamento-e-retencao/spec.md) | #9 | Done | Arquivo JSON local, retenção configurável, entradas de agenda criptografadas e execução em instância única |
| [`20261106-protecao-dns-rebinding-wordpress/`](specs/20261106-protecao-dns-rebinding-wordpress/spec.md) | — | Done | DNS público validado e rejeição de resolução insegura antes da conexão; testes cobrem rebinding |
| [`20261106-api-rest-openapi/`](specs/20261106-api-rest-openapi/spec.md) | #9 | Done | API local autenticada com bearer key e OpenAPI; `127.0.0.1` por padrão |
| [`20261106-ai-init-adocao-v2/`](specs/20261106-ai-init-adocao-v2/spec.md) | poc-collector#19/#20; ai-init-internal#277/#278 | Done | Adoção v2 `85c264c` integrada em develop e main nos dois repositórios; 43 testes e doc-check local passaram |
| [`20261110-atualizar-java-27/`](specs/20261110-atualizar-java-27/spec.md) | poc-collector#11, #12, #18 | Done | Java 27 standalone integrado em main; `mvn -B -ntp clean verify` passou com 43 testes; API smoke verificou health 200, auth 401/200 e bind loopback |
| [`20261111-poc-collector-webscrapper/`](specs/20261111-poc-collector-webscrapper/spec.md) | — | Draft | Bootstrap AI-init/internal; Hacker News + WordPress News nos dois módulos; fase 1 parser→JSON (`entities: []`), fase 2 regras e depois avaliação LLM; IDs nativos e fallback fingerprint composto definidos; specs downstream para quatro projetos acordadas; URL de item e interface de serviço em entrevista |

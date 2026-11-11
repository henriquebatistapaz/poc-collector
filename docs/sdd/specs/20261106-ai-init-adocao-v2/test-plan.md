# Plano de teste — ai-init-adocao-v2 (poc-collector)

## Critérios de aceite

| Critério | Verificação | Resultado |
|---|---|---|
| AC-01 | `.ai-init.json` aponta para `85c264c`; SHA-256 dos 34 assets gerenciados conferem no contexto central e no projeto | Aprovado |
| AC-02 | `bash .ai-init/doc-check.sh` | Aprovado — OK, 0 avisos |
| AC-03 | Guard bloqueia `.env` e permite `.env.example` | Aprovado — exit 2 e exit 0 |
| AC-04 | AGENTS não contém placeholders `{...}` | Aprovado — nenhum |
| AC-05 | Gitleaks e revisão dos workflows/secrets | Aprovado — nenhuma credencial; review report-only e secrets opcionais |
| RF-04 | `bash .ai-init/agent-setup.sh` | Aprovado — imprimiu `Agent environment ready.` em Java 27 |
| Build | `mvn -B -ntp clean verify` | Aprovado — 43 testes, 0 falhas/erros/ignorados |

## Casos executados

1. `bash .ai-init/doc-check.sh` — OK, sem avisos.
2. `guard.sh pre-edit` em `.env` — exit 2; `.env.example` — exit 0.
3. `bash .ai-init/agent-setup.sh` — ambiente pronto.
4. Shell/YAML syntax, manifesto/hash, placeholders e `git diff --check` — passaram.
5. `gitleaks detect --no-git --source . --redact --no-banner` — nenhuma detecção.
6. Revisão estática: workflows usam permissões mínimas, mantêm AI review em report-only e ignoram execução de IA sem `ANTHROPIC_API_KEY`; private context é opcional via `AI_INIT_INTERNAL_TOKEN`. Nenhum secret foi criado.

Os checks hospedados do GitHub não reportaram jobs nesta execução. A integração Reddit foi posteriormente cancelada pelo proprietário em 2026-11-11 e removida; este plano registra a validação anterior ao cancelamento.

# SDD: renomeacao-collector-mini

**Complexity:** S

> SDD retroativa: documenta o que já foi entregue (commits 3c70f66, 2fa3b38, a9129e0, 811e326, 61d1218). Escrita em 2026-11-06 a partir do código e do histórico do repo; status `Done`.

## 1. Context
O projeto nasceu como poc-multi-source-collector e os consumidores mudaram de nome.

## 2. Goal
Alinhar nome, pacote e docs.

## 3. Scope
### 3.1 In scope
- Projeto `poc-collector`, pacote `com.henrique.collector`.
- Remoção das menções ao Mercado Livre.
- Referências a poc-dashboard e poc-collector-runner.

### 3.2 Out of scope
- —

## 4. Requirements
### 4.1 Functional
- FR-01: Artefato e pacote com o nome novo.

### 4.2 Non-functional
- —

## 5. Architecture and Design
Refatoração mecânica.

## 6. Implementation Plan
Já implementado. Principais pontos no código:
- `pom.xml`
- `src/main/java/com/henrique/collector/**`
- `README.md`

## 7. Acceptance Criteria
- AC-01: `mvn test` verde após a renomeação.

## 8. Observability and Operations
- Sem requisitos específicos além dos logs padrão da aplicação.

## 9. Risks and Mitigations
- —

## 10. Rollback
Reverter os commits/PRs listados nas referências.

## 11. References
- commits 3c70f66, 2fa3b38, a9129e0, 811e326, 61d1218

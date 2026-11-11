# SDD: rota-oficial-issuu

**Complexity:** S

> Decisão aprovada e registrada em 2026-11-11: fonte continua desativada para descoberta pública.

## 1. Context
A fonte Issuu está desligada por falta de rota oficial.

## 2. Goal
Pesquisar rota permitida do Issuu.

## 3. Scope
### 3.1 In scope
- Pesquisa da API/termos.

### 3.2 Out of scope
- Scraping.

## 4. Requirements
### 4.1 Functional
- FR-01: Fonte só é ligada com rota oficial verificada.

### 4.2 Non-functional
- A definir na revisão.

## 5. Architecture and Design
Issuu mantém [SDK oficial de API](https://github.com/issuu/issuu-api-sdk) para ações de conta autenticada, como listar publicações do usuário autenticado e consultar uma publicação por slug. Não há endpoint oficial documentado para pesquisar publicações públicas arbitrárias nem feed público de descoberta. A API existente não atende à coleta aberta planejada; não implementar acesso por endpoints ocultos nem scraping.

## 6. Implementation Plan
Manter `ISSUU` como `UNAVAILABLE` no registry e rejeitar tentativas de coleta. Reabrir apenas quando Issuu documentar uma rota pública de descoberta adequada ou quando o escopo for alterado para usar credenciais de uma conta própria.

## 7. Acceptance Criteria
- AC-01: Decisão documentada e fonte mantida desativada sem rota pública de descoberta verificada.

## 8. Observability and Operations
- Verificar o SDK oficial de Issuu antes de reconsiderar a implementação.

## 9. Risks and Mitigations
- A definir.

## 10. Rollback
A definir.

## 11. References
- TODO.md do projeto

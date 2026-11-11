# SDD: protecao-dns-rebinding-wordpress

**Complexity:** M

> Aprovada e implementada em `develop`; cobertura em `WordPressFeedCollectorTest`.

## 1. Context
A coleta WordPress precisa de proteção mais forte contra DNS rebinding antes de virar serviço hospedado.

## 2. Goal
Endurecer a checagem de destino público.

## 3. Scope
### 3.1 In scope
- Revalidação de DNS por conexão e redirect.

### 3.2 Out of scope
- —

## 4. Requirements
### 4.1 Functional
- FR-01: Destino que muda para IP privado é bloqueado.

### 4.2 Non-functional
- A definir na revisão.

## 5. Architecture and Design
O coletor valida HTTPS e resolve o host para endereços públicos. O fetcher fixa os endereços verificados para a conexão e a política rejeita uma nova resolução não pública antes da conexão, bloqueando mudança para endereço privado.

## 6. Implementation Plan
Política de URL pública e resolução validada aplicadas antes de cada requisição WordPress; cobertura com resolver sintético que alterna endereço público/privado.

## 7. Acceptance Criteria
- AC-01: Teste de rebinding falha sem a proteção.

## 8. Observability and Operations
- Manter redirects desabilitados e limites de resposta/tempo para impedir que a validação seja contornada por redirecionamentos.

## 9. Risks and Mitigations
- A definir.

## 10. Rollback
A definir.

## 11. References
- TODO.md do projeto (Later implementation slices)

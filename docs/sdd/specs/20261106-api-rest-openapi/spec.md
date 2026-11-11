# SDD: api-rest-openapi

**Complexity:** M

> Aprovada e implementada em `develop` (PR #9), incluindo revisão de ameaças.

## 1. Context
O poc-collector só tem CLI.

## 2. Goal
Expor endpoints REST com OpenAPI depois da revisão de ameaças.

## 3. Scope
### 3.1 In scope
- Endpoints REST + OpenAPI.

### 3.2 Out of scope
- —

## 4. Requirements
### 4.1 Functional
- FR-01: Coleta disponível via REST.

### 4.2 Non-functional
- A definir na revisão.

## 5. Architecture and Design
O serviço standalone oferece `GET /health`, `GET /openapi.json`, `GET /v1/sources`, `POST /v1/collections`, `GET /v1/items`, listagem/cadastro/remoção de feeds e chave bearer para `/v1/*`. O servidor liga em `127.0.0.1:8090` por padrão. O dashboard permanece consumidor separado da biblioteca; a API atende automação local e integração independente.

## 6. Implementation Plan
Implementado em `develop` pelo PR #9, com acesso protegido e escopo de bind local seguro.

## 7. Acceptance Criteria
- AC-01: Swagger documenta os endpoints.

## 8. Observability and Operations
- Exposição remota requer proxy TLS e bind explícito a interface de rede; a chave precisa ter pelo menos 32 bytes.

## 9. Risks and Mitigations
- A definir.

## 10. Rollback
A definir.

## 11. References
- TODO.md do projeto

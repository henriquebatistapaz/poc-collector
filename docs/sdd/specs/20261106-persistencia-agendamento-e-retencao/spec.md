# SDD: persistencia-agendamento-e-retencao

**Complexity:** L

> Aprovada e implementada em `develop` (PR #9).

## 1. Context
Persistência, agendamento e retenção ainda não foram aprovados.

## 2. Goal
Definir requisitos de API, persistência, agendamento e retenção.

## 3. Scope
### 3.1 In scope
- Requisitos e contrato.

### 3.2 Out of scope
- —

## 4. Requirements
### 4.1 Functional
- FR-01: Retenção e exclusão definidas antes de persistir.

### 4.2 Non-functional
- A definir na revisão.

## 5. Architecture and Design
O serviço standalone guarda itens normalizados e jobs em arquivo JSON local (`COLLECTOR_DATA_FILE`), executa uma instância e aplica retenção configurável por `COLLECTOR_RETENTION_DAYS` (padrão 30, intervalo 1–3650). Entradas de agenda são criptografadas com AES-GCM e jobs repetitivos aceitam intervalos entre 5 minutos e 24 horas. O dashboard pode manter seu próprio estado/configuração.

## 6. Implementation Plan
Implementado e integrado no PR #9.

## 7. Acceptance Criteria
- AC-01: Requisitos aprovados e registrados.

## 8. Observability and Operations
- Para containers, montar volume persistente e manter backup do arquivo. Não compartilhar o mesmo arquivo entre processos concorrentes.

## 9. Risks and Mitigations
- A definir.

## 10. Rollback
A definir.

## 11. References
- TODO.md do projeto

# Plano de testes — Java 27 no poc-collector

## Revisões e ambiente

- Baseline extraído de origin/develop: acc7df634df1e5ce84d381b904b5c3e346230f63.
- Candidata integrada em main (PRs poc-collector #11, #12 e #18); a branch local antiga foi superada.
- Baseline: OpenJDK 21.0.13.0.3 (Homebrew), Maven 3.10.0.
- Candidata: OpenJDK 27.0.1.0.3 (Homebrew), Maven 3.10.0.
- Repositórios Maven isolados: /tmp/poc-java27-baseline-exact/m2 e /tmp/poc-java27-candidate-m2.
- Sem credenciais reais ou chamadas a provedores externos; o smoke externo existente permanece ignorado.

## Execuções e resultados

| Etapa | Comando e ambiente | Resultado |
|---|---|---|
| Linha de base | JDK 21, mvn clean install (dashboard: clean verify) | PASSOU; 41 testes, 0 falhas/erros, 0 ignorados. |
| Candidata Java 27 | JDK 27, mvn clean install | PASSOU; 41 testes, 0 falhas/erros, 0 ignorados. |
| Bytecode | javap -verbose nas classes Java 21/27 | Baseline major version 65; candidata major version 71. |
| Segurança operacional | revisão do diff, logs e portas/binds | PRs Java 27 só alteraram POM/README/Docker; API continua em `127.0.0.1` por padrão e não houve mudança de rotas/binds. |

## Resultado por critério

- AC-01a: atendido. Os builds usaram OpenJDK 27.0.1.0.3; as classes candidatas verificadas têm major version 71.
- AC-01b: atendido para este repositório; `mvn -B -ntp clean verify` passou com 43 testes. Cópias no runner/dashboard têm seus próprios gates.
- AC-02: atendido para os gates determinísticos: 41 testes passaram em ambas as linhas. AC-03: smoke atual confirmou bind loopback e proteção bearer. AC-04: baseline JDK 21 passou, mas rollback pós-candidata não foi executado.
- AC-03: atendido por revisão do diff e execução dos testes: a migração não alterou rotas nem bind; o serviço continua loopback por padrão.
- AC-04: atendido por revisão do diff: nenhuma alteração de esquema ou formato de dados; retorno consiste em restaurar o POM/runtime anterior. O baseline Java 21 passou com 41 testes.

## Limitações

Não houve alteração de imagem ou deploy nesta migração; os testes de runtime do serviço standalone foram cobertos pelo suite local. A verificação visual do dashboard pertence à spec de deploy do poc-dashboard.

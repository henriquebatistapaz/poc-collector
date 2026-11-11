# SDD: acesso-a-reddit-data-api

**Status:** Cancelled by the user on 2026-11-11.

## Decisão

O proprietário cancelou a integração Reddit. A aplicação OAuth, o coletor, o transporte HTTP, o comando CLI, o registro da fonte e os testes específicos foram removidos do `poc-collector`. Não solicitar acesso externo nem configurar credenciais para este projeto.

## Dados locais existentes

Itens Reddit já armazenados permanecem legíveis como histórico. Agendas Reddit existentes são descartadas ao carregar o arquivo de estado, para impedir novas execuções. Nenhuma requisição à rede Reddit é feita pelo projeto.

## Retomada futura

Qualquer retomada exige uma nova spec aprovada pelo proprietário. Esta spec permanece no índice apenas como registro da decisão de cancelamento.

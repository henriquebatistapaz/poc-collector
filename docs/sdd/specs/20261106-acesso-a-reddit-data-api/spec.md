# SDD: acesso-a-reddit-data-api

**Complexity:** S
**Approval:** Aprovada pelo usuário em 2026-11-11.
**Status:** Blocked — a integração ao vivo depende de aprovação explícita da Reddit e credenciais OAuth runtime.

## 1. Contexto

O adaptador OAuth e o leitor da API oficial estão implementados com fixtures locais. O uso ao vivo permanece desativado até que Reddit aprove o app e o caso de uso; nenhuma rota sem token, scraping ou endpoint não documentado será usada.

## 2. Objetivo

Habilitar coleta do Reddit exclusivamente pela Data API oficial, após aprovação externa e configuração segura das credenciais.

## 3. Escopo

### 3.1 Incluído
- Solicitar acesso usando a conta oficial do responsável pelo app e o caso de uso real.
- Registrar o aplicativo OAuth conforme a documentação da Reddit.
- Guardar client ID, client secret e refresh token apenas no secret store local/runtime.
- Executar um smoke test controlado após a aprovação.

### 3.2 Fora de escopo
- Acesso a Reddit antes da aprovação explícita.
- Scraping HTML, cookies, endpoints ocultos ou contorno de limites.
- Armazenar credenciais em arquivos versionados.

## 4. Requisitos

- RF-01: coleta ao vivo só funciona com `REDDIT_API_ACCESS_APPROVED=true` e credenciais OAuth válidas.
- RF-02: sem aprovação/credenciais, o adaptador falha fechado e não faz requisições ao Reddit.
- RNF-01: requests usam o User-Agent descritivo e os limites definidos na documentação oficial.
- RNF-02: logs e testes não expõem tokens ou segredos.

## 5. Arquitetura

`RedditOAuthAuthorization`, `RedditOAuthTokenProvider`, `RedditCollector` e o transporte oficial são isolados atrás do registro de fontes. Os testes usam respostas locais; o runtime continua desativado enquanto o gate oficial não estiver satisfeito.

## 6. Plano

1. Responsável solicita a aprovação pela via oficial da Reddit com a descrição verdadeira do caso de uso.
2. Só após a resposta positiva, registra app OAuth e guarda as credenciais no ambiente local/secret store.
3. Confirma `REDDIT_API_ACCESS_APPROVED=true` fora do repositório.
4. Executa `mvn -B -ntp clean verify` e um smoke test limitado, registra resultado sem dados sensíveis.
5. Se a aprovação não for concedida, mantém Reddit desativado e usa apenas fontes com acesso permitido.

## 7. Critérios de aceite

- AC-01: sem gate e credenciais, a fonte permanece desabilitada e nenhum request é enviado.
- AC-02: testes offline cobrem token, estado OAuth, leitura e erros sem acessar a rede Reddit.
- AC-03: após a aprovação, smoke controlado confirma resposta oficial, respeitando limites e removendo segredos dos logs.
- AC-04: nenhuma credencial ou conteúdo real é incluído em Git, plano de teste ou logs.

## 8. Risco e mitigação

A aprovação pode ser recusada ou limitada. Nesse caso a fonte continua desabilitada; não se tenta substituir a aprovação por scraping ou métodos não documentados.

## 9. Referências oficiais

- [Reddit Responsible Builder Policy](https://support.reddithelp.com/hc/en-us/articles/16471395473812-Moderation-Bots-Tooling) — exige solicitação e aprovação explícita antes de acessar dados pela API.
- [Reddit Data API Terms](https://redditinc.com/policies/data-api-terms) — exige usar somente os métodos de acesso e credenciais descritos na documentação oficial.
- [Developer Platform & Accessing Reddit Data](https://support.reddithelp.com/hc/en-us/articles/14945211791892-Developer-Platform-Accessing-Reddit-Data).

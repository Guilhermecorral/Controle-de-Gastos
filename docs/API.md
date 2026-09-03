# Referência da API

Esta referência resume a API REST da versão 1.3.3. O contrato executável completo pode ser consultado pelo Swagger UI no ambiente de desenvolvimento.

## Acesso

| Ambiente | Base |
| --- | --- |
| Desenvolvimento direto | `http://localhost:8080` |
| Desenvolvimento pelo Vite | `http://localhost:5173/api` |
| Produção | `https://www.farolfinanceiro.online/api` |

As rotas abaixo são apresentadas com o prefixo `/api`, exceto os endpoints operacionais `/ping` e `/actuator/health`.

## Autenticação

A autenticação usa JWT armazenado em cookies `HttpOnly`. Clientes de navegador devem enviar credenciais nas requisições. Ferramentas de linha de comando precisam manter o arquivo de cookies entre login, renovação e chamadas protegidas.

Exemplo:

```bash
curl -i -c cookies.txt \
  -H "Content-Type: application/json" \
  -d '{"email":"usuario@exemplo.com","password":"senha"}' \
  http://localhost:8080/api/auth/login

curl -b cookies.txt http://localhost:8080/api/dashboard
```

Se a conta possuir 2FA, o fluxo de login exige o código TOTP conforme indicado pela resposta de autenticação.

## Formato de erro

Erros tratados seguem esta estrutura:

```json
{
  "timestamp": "2026-09-03T12:00:00-03:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Dados inválidos",
  "path": "/api/transactions",
  "details": ["amount: deve ser maior que zero"]
}
```

Respostas comuns:

| Status | Significado |
| --- | --- |
| `200` | Consulta ou atualização concluída |
| `201` | Recurso criado |
| `204` | Operação concluída sem corpo |
| `400` | Entrada inválida ou regra de negócio não atendida |
| `401` | Sessão ausente, inválida ou expirada |
| `403` | Usuário autenticado sem permissão |
| `404` | Recurso não encontrado |
| `409` | Conflito com o estado atual ou dado duplicado |
| `429` | Limite de requisições excedido |
| `500` | Falha interna não esperada |

## Autenticação e conta

| Método | Rota | Descrição |
| --- | --- | --- |
| `POST` | `/api/auth/register` | Cria uma conta |
| `POST` | `/api/auth/login` | Autentica e cria a sessão |
| `GET` | `/api/auth/me` | Retorna o usuário da sessão |
| `POST` | `/api/auth/refresh` | Renova o token de acesso |
| `POST` | `/api/auth/logout` | Encerra a sessão e limpa cookies |
| `POST` | `/api/auth/forgot-password` | Solicita recuperação de senha |
| `POST` | `/api/auth/reset-password` | Define uma nova senha com token válido |
| `GET` | `/api/auth/reset-password/redirect?token=...` | Redireciona o link recebido por e-mail |
| `PUT` | `/api/users/me` | Atualiza o perfil autenticado |
| `DELETE` | `/api/users/me` | Exclui a própria conta |
| `GET` | `/api/users/me/two-factor` | Consulta o estado do 2FA |
| `POST` | `/api/users/me/two-factor/setup` | Inicia a configuração TOTP |
| `POST` | `/api/users/me/two-factor/confirm` | Confirma e ativa o TOTP |
| `POST` | `/api/users/me/two-factor/disable` | Desativa o TOTP após validação |

## Painel e análise mensal

| Método | Rota | Descrição |
| --- | --- | --- |
| `GET` | `/api/dashboard` | Retorna os indicadores gerais |
| `GET` | `/api/dashboard?year=2026&month=9` | Filtra o painel por competência |
| `GET` | `/api/monthly-analysis?year=2026&month=9` | Retorna a análise mensal consolidada |

`year` e `month` são obrigatórios na análise mensal. No dashboard, ambos são opcionais.

## Transações e comprovantes

| Método | Rota | Descrição |
| --- | --- | --- |
| `POST` | `/api/transactions` | Cria receita ou despesa |
| `GET` | `/api/transactions` | Lista transações do usuário |
| `GET` | `/api/transactions?type=...&category=...` | Filtra por tipo e categoria |
| `PUT` | `/api/transactions/{id}` | Atualiza uma transação |
| `DELETE` | `/api/transactions/{id}` | Exclui uma transação |
| `POST` | `/api/transactions/import` | Importa um conjunto estruturado de transações |
| `POST` | `/api/ofx/upload` | Importa extrato OFX, CSV, TSV, XLS ou XLSX |
| `POST` | `/api/transactions/{id}/receipt` | Anexa um comprovante à transação |
| `POST` | `/api/transactions/receipts/preview` | Analisa arquivos antes da associação |
| `GET` | `/api/transactions/receipts?year=2026&month=9` | Lista comprovantes da competência |
| `GET` | `/api/transactions/{id}/receipt/download` | Baixa o comprovante vinculado |

Uploads usam `multipart/form-data`. O campo do extrato é `file`, o campo do anexo individual é `file` e a análise em lote usa `files`.

## Investimentos

| Método | Rota | Descrição |
| --- | --- | --- |
| `GET` | `/api/investments/portfolio` | Retorna posições consolidadas e totais |
| `POST` | `/api/investments/positions` | Cria uma posição |
| `PUT` | `/api/investments/positions/{id}` | Atualiza uma posição |
| `DELETE` | `/api/investments/positions/{id}` | Remove uma posição |
| `GET` | `/api/investments/movements` | Lista movimentações |
| `POST` | `/api/investments/movements/trades` | Registra compra ou venda |
| `POST` | `/api/investments/positions/{id}/income` | Registra provento em uma posição |
| `GET` | `/api/investments/income-schedules` | Lista a agenda de proventos |
| `POST` | `/api/investments/income-schedules` | Agenda provento com Data Com, pagamento e imposto |
| `POST` | `/api/investments/income-schedules/{id}/receive` | Confirma o recebimento de um provento agendado |
| `GET` | `/api/investments/goals` | Lista metas de patrimônio e seus saldos próprios |
| `POST` | `/api/investments/goals` | Cria uma meta de patrimônio |
| `PUT` | `/api/investments/goals/{id}` | Edita objetivo, valor inicial, aporte previsto e taxa |
| `POST` | `/api/investments/goals/{id}/contributions` | Registra um aporte exclusivo da meta |
| `GET` | `/api/investments/assets/search?query=BBAS&type=ACAO` | Pesquisa o catálogo |
| `GET` | `/api/investments/quotes` | Consulta uma cotação |
| `GET` | `/api/investments/projections` | Calcula uma projeção de renda fixa |

Pesquisa de ativos:

- `query`: código ou parte do nome.
- `type`: `ACAO`, `FII`, `CRIPTO` ou `RENDA_FIXA`.

Consulta de cotação:

- `type`: obrigatório.
- `symbol`: símbolo do ativo quando aplicável.
- `externalId`: identificador do provedor quando aplicável.
- `market`: mercado, com padrão `BR`.

Simulação de renda fixa:

| Parâmetro | Obrigatório | Descrição |
| --- | --- | --- |
| `initialAmount` | Não | Valor inicial da simulação |
| `monthlyContribution` | Não | Aporte mensal, padrão `0` |
| `interestRate` | Não | Taxa informada pelo usuário |
| `ratePeriod` | Não | `MONTHLY` ou `ANNUAL`; padrão `ANNUAL` |
| `timelinePeriod` | Não | Resultado por `MONTHLY` ou `YEARLY`; padrão `MONTHLY` |
| `startDate` | Não | Data inicial em `AAAA-MM-DD` |
| `endDate` | Não | Data final em `AAAA-MM-DD` |

`principal`, `annualRate` e `maturityDate` são mantidos temporariamente para compatibilidade com o simulador anterior. Novos clientes devem usar os parâmetros da tabela.

Exemplo:

```bash
curl -G -b cookies.txt \
  --data-urlencode "initialAmount=1000" \
  --data-urlencode "monthlyContribution=250" \
  --data-urlencode "interestRate=1" \
  --data-urlencode "ratePeriod=MONTHLY" \
  --data-urlencode "timelinePeriod=MONTHLY" \
  --data-urlencode "startDate=2026-09-01" \
  --data-urlencode "endDate=2027-09-01" \
  http://localhost:8080/api/investments/projections
```

A resposta separa o total investido dos juros ganhos e inclui a evolução por período para montagem da tabela e dos gráficos.

## Lista de desejos

| Método | Rota | Descrição |
| --- | --- | --- |
| `GET` | `/api/wishlist/lists` | Lista as coleções do usuário |
| `POST` | `/api/wishlist/lists` | Cria uma coleção |
| `PUT` | `/api/wishlist/lists/{id}` | Atualiza uma coleção |
| `DELETE` | `/api/wishlist/lists/{id}` | Exclui uma coleção |
| `GET` | `/api/wishlist` | Lista itens e aceita filtros |
| `POST` | `/api/wishlist` | Cria um item |
| `PUT` | `/api/wishlist/{id}` | Atualiza um item |
| `DELETE` | `/api/wishlist/{id}` | Exclui um item |
| `GET` | `/api/wishlist/summary` | Retorna totais resumidos |
| `GET` | `/api/wishlist/{id}/history` | Retorna o histórico do item |
| `POST` | `/api/wishlist/{id}/purchase` | Converte o item em compra |
| `POST` | `/api/wishlist/{id}/undo-purchase` | Desfaz a conversão em compra |
| `POST` | `/api/wishlist/import/preview` | Analisa arquivo de importação |
| `POST` | `/api/wishlist/import` | Confirma a importação analisada |

A listagem aceita `status`, `sortBy` e `listId` como parâmetros opcionais.

## Administração

Todas as rotas exigem permissão administrativa.

| Método | Rota | Descrição |
| --- | --- | --- |
| `GET` | `/api/admin/overview` | Indicadores operacionais |
| `GET` | `/api/admin/users` | Lista usuários |
| `PATCH` | `/api/admin/users/{userId}/status` | Ativa ou desativa um usuário |
| `PATCH` | `/api/admin/users/{userId}/role` | Altera o papel de acesso |
| `POST` | `/api/admin/users/{userId}/reset-password` | Redefine a senha sob política administrativa |
| `POST` | `/api/admin/users/{userId}/reset-two-factor` | Remove a configuração 2FA |

## Endpoints operacionais

| Método | Rota | Descrição |
| --- | --- | --- |
| `GET` | `/ping` | Verificação simples de disponibilidade |
| `GET` | `/actuator/health` | Estado de saúde da aplicação |

Não exponha endpoints adicionais do Actuator publicamente sem revisar autenticação e conteúdo retornado.

## Compatibilidade

Alterações aditivas em DTOs são preferíveis. Remoções, renomeações e mudanças de semântica devem ter período de compatibilidade e registro no `CHANGELOG.md`. O frontend e a API são publicados como uma única versão funcional e devem ser testados em conjunto.

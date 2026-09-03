# Documentação do Farol Financeiro

Este diretório reúne a documentação técnica e operacional do projeto. O `README.md` da raiz apresenta o produto; os documentos daqui explicam como ele funciona, como desenvolver com segurança e como operá-lo.

## Mapa da documentação

| Documento | Finalidade | Público principal |
| --- | --- | --- |
| [Arquitetura](ARCHITECTURE.md) | Componentes, módulos, dados, integrações e decisões técnicas | Desenvolvimento e revisão técnica |
| [Desenvolvimento](DEVELOPMENT.md) | Instalação, configuração, comandos, testes e migrações | Quem vai executar ou alterar o projeto |
| [API](API.md) | Autenticação, convenções e catálogo de endpoints | Frontend, backend e integrações |
| [Runbook de produção](PRODUCTION_DEPLOY_RUNBOOK.md) | Deploy, domínio, sessão, Vercel, Render e diagnóstico | Operação e manutenção de produção |
| [Produção com Docker](../infra/deploy/PRODUCAO.md) | Implantação própria com Nginx, TLS e backup | Operação em VPS |
| [Changelog](../CHANGELOG.md) | Mudanças entregues por versão | Todos os colaboradores |

## Qual documentação usar

- Para conhecer o projeto e executá-lo rapidamente, comece pelo [README principal](../README.md).
- Para entender uma alteração antes de programar, consulte [Arquitetura](ARCHITECTURE.md) e [API](API.md).
- Para preparar o ambiente ou criar migrações, use [Desenvolvimento](DEVELOPMENT.md).
- Para publicar ou investigar produção, siga o [runbook](PRODUCTION_DEPLOY_RUNBOOK.md).

## Regra de manutenção

Uma mudança deve atualizar a documentação quando alterar qualquer um destes pontos:

- contrato ou comportamento da API;
- variável de ambiente;
- dependência externa;
- modelo ou migração de banco;
- procedimento de execução, teste ou deploy;
- regra de segurança ou privacidade;
- funcionalidade apresentada no README.

Atualizações relevantes ao usuário também devem entrar no [CHANGELOG.md](../CHANGELOG.md).


# Guia de desenvolvimento

Este guia prepara o ambiente local, reúne os comandos mais usados e define cuidados para contribuir com o Farol Financeiro.

## Pré-requisitos

- Git
- Java 21
- Node.js 20 ou superior
- npm 10 ou superior
- Docker Desktop, opcional para PostgreSQL e Redis

Confirme as versões:

```bash
java -version
node --version
npm --version
docker --version
```

## Início rápido com H2

Na raiz do repositório, prepare as variáveis locais do backend:

```powershell
Copy-Item backend/back/.env.example backend/back/.env
```

Revise os valores copiados. Nunca reutilize segredos de produção no computador de desenvolvimento.

Inicie o backend:

```powershell
cd backend/back
./mvnw spring-boot:run
```

Em outro terminal, inicie o frontend:

```powershell
cd frontend
npm ci
npm run dev
```

Serviços locais:

| Serviço | Endereço |
| --- | --- |
| Frontend | `http://localhost:5173` |
| API | `http://localhost:8080` |
| Swagger UI | `http://localhost:8080/swagger-ui/index.html` |
| Health check | `http://localhost:8080/actuator/health` |
| Ping | `http://localhost:8080/ping` |

O Vite encaminha requisições iniciadas por `/api` para a porta `8080`.

## Ambiente com Docker

Use PostgreSQL e Redis quando estiver trabalhando com migrações, diferenças de banco ou rate limit distribuído:

```powershell
docker compose --env-file backend/back/.env -f Docker-compose.yml up --build
```

O compose local sobe a infraestrutura e o backend. Execute o frontend separadamente com `npm run dev`.

Para encerrar:

```powershell
docker compose --env-file backend/back/.env -f Docker-compose.yml down
```

Não use `down -v` sem intenção explícita, pois essa opção remove os volumes e os dados locais.

## Perfis do Spring

| Perfil | Uso | Banco |
| --- | --- | --- |
| `dev` | Desenvolvimento rápido | H2 em memória |
| `docker` | Desenvolvimento integrado | PostgreSQL |
| `prod` | Produção | PostgreSQL com Flyway |
| `test` | Testes automatizados | Configuração isolada de teste |

As configurações estão em `backend/back/src/main/resources/application-*.yaml`.

## Variáveis de ambiente

Os arquivos de exemplo são a fonte de referência para nomes e comentários:

- `.env.production.example`: contrato de configuração da implantação completa.
- `backend/back/.env.example`: valores esperados pelo backend e pelo compose local.

Os principais grupos são:

| Grupo | Finalidade |
| --- | --- |
| Banco | URL JDBC, usuário e senha do PostgreSQL |
| JWT e cookies | Assinatura, duração e atributos da sessão |
| CORS | Origens confiáveis do frontend |
| 2FA | Chave usada para cifrar segredos TOTP |
| E-mail | Entrega de recuperação de senha |
| CAPTCHA | Verificação antiabuso |
| Redis | Rate limit compartilhado |
| Mercado | Tokens e URLs dos provedores de cotação |
| Storage | Diretório persistente de comprovantes |

Ao adicionar uma variável, atualize o arquivo de exemplo e a documentação na mesma alteração. Nunca inclua o valor real de um segredo.

## Comandos do frontend

Execute em `frontend/`:

```bash
npm run dev
npm run build
npm run preview
```

`npm run lint` existe no projeto, mas depende da inclusão de uma configuração ESLint compatível antes de ser usado como validação obrigatória.

Ainda não há um comando de testes automatizados do frontend no `package.json`. Até a inclusão de um test runner, `npm run build` é a validação automatizada mínima dessa camada.

## Comandos do backend

Execute em `backend/back/`:

```powershell
./mvnw spring-boot:run
./mvnw test
./mvnw clean package
```

No Windows, o wrapper Maven deve ser preferido para evitar divergência de versão entre máquinas.

## Banco e migrações

As migrações Flyway ficam em `backend/back/src/main/resources/db/migration/`.

Para criar uma alteração:

1. Descubra a próxima versão disponível.
2. Crie `V<n>__descricao_curta.sql`.
3. Escreva uma migração compatível com PostgreSQL.
4. Suba a aplicação com o perfil Docker.
5. Execute os testes do backend.
6. Nunca edite uma migração que já foi aplicada em ambiente compartilhado.

Alterações de entidade JPA e alteração de esquema devem chegar juntas. O ambiente de produção valida as migrações em vez de alterar o banco silenciosamente.

## Fluxo de implementação

Para novas funcionalidades, mantenha o caminho completo verificável:

1. Defina o contrato de entrada e saída.
2. Implemente e teste a regra de negócio no backend.
3. Exponha o endpoint com validação e autorização.
4. Crie o serviço HTTP e os tipos no frontend.
5. Implemente estados de carregamento, sucesso, vazio e erro.
6. Valide desktop, mobile e acessibilidade básica.
7. Atualize documentação e changelog quando houver impacto público.

## Convenções importantes

- Valores monetários no backend usam `BigDecimal`; evite `double` em cálculos financeiros.
- Datas civis usam `LocalDate`; instantes e auditoria devem possuir fuso ou offset explícito.
- Controllers não devem conter regras financeiras extensas.
- Acesso a dados de outro usuário deve ser impedido no service ou na consulta ao repository.
- O frontend deve consumir `/api`, sem fixar a URL pública do backend nos componentes.
- Mensagens de erro destinadas ao usuário devem ser claras e não revelar detalhes internos.
- Componentes novos devem seguir a identidade visual existente do Farol Financeiro.

## Testes

Antes de abrir uma alteração para revisão, execute:

```powershell
cd backend/back
./mvnw test
cd ../../frontend
npm run build
```

Prioridades de cobertura:

- autenticação, autorização e isolamento entre usuários;
- cálculos de saldo, custo médio, rentabilidade e projeções;
- compra, venda e registro de proventos;
- importação e prevenção de duplicidades;
- validação de arquivos e comprovantes;
- estados críticos dos formulários no frontend.

## Diagnóstico rápido

### A porta 8080 está ocupada

Identifique o processo antes de encerrá-lo:

```powershell
Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue
```

### O frontend recebe erro de conexão

Confirme se o backend responde em `http://localhost:8080/ping` e se a chamada do frontend usa o prefixo `/api`.

### O banco não inicia no Docker

Verifique os containers e logs:

```powershell
docker compose --env-file backend/back/.env -f Docker-compose.yml ps
docker compose --env-file backend/back/.env -f Docker-compose.yml logs backend postgres redis
```

### A cotação não aparece

Confira a fonte e a mensagem devolvidas pela API. Falhas de provedor não devem apagar a posição existente; o sistema pode recorrer ao catálogo local ou informar indisponibilidade temporária.

### O login funciona na API, mas não no navegador

Revise origem permitida, HTTPS, domínio e atributos dos cookies. Em requisições manuais no frontend, `credentials: 'include'` precisa estar habilitado.

## Preparação de release

1. Atualize a versão em `frontend/package.json` e nos metadados correspondentes.
2. Registre mudanças relevantes em `CHANGELOG.md`.
3. Execute testes e builds completos.
4. Valide migrações em PostgreSQL.
5. Siga o [runbook de produção](PRODUCTION_DEPLOY_RUNBOOK.md).

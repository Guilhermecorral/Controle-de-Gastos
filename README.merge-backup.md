# 💰 Controle de Gastos

> Sistema de controle financeiro pessoal com autenticação segura via JWT, relatórios em tempo real e análise mensal comparativa.

![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.3-brightgreen?style=flat-square&logo=springboot)
![Spring Security](https://img.shields.io/badge/Spring%20Security-7-brightgreen?style=flat-square&logo=springsecurity)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?style=flat-square&logo=postgresql)
![Docker](https://img.shields.io/badge/Docker-Compose-blue?style=flat-square&logo=docker)
![React](https://img.shields.io/badge/React-18-61DAFB?style=flat-square&logo=react)
![TypeScript](https://img.shields.io/badge/TypeScript-5-3178C6?style=flat-square&logo=typescript)
![License](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)

---

## 📋 Sobre o Projeto

O **Controle de Gastos** é uma aplicação web full-stack desenvolvida para resolver um problema real de organização financeira pessoal. O sistema permite registrar receitas e despesas, analisar padrões de consumo mensais, gerenciar uma lista de desejos e visualizar relatórios comparativos — tudo com autenticação segura e interface moderna.

Projeto desenvolvido como **portfólio técnico**, demonstrando domínio de tecnologias de mercado em todas as camadas da aplicação.

---

## ✨ Funcionalidades

### Implementadas ✅
- **Autenticação segura** — Registro e login com JWT (Access Token 15min + Refresh Token 7 dias)
- **Criptografia de senha** — BCrypt com salt automático
- **Transações CRUD** — Criar, listar, editar e deletar receitas e despesas
- **Filtros avançados** — Por tipo (Receita/Despesa) e categoria
- **Isolamento de dados** — Cada usuário só acessa seus próprios dados
- **Roles de usuário** — USER e ADMIN
- **API documentada** — Swagger UI disponível em `/swagger-ui.html`

### Em desenvolvimento 🔧
- **Dashboard** — Saldo atual, total de receitas/despesas e últimas transações
- **Análise Mensal** — Relatório por período com comparativo percentual vs mês anterior
- **Lista de Desejos** — Itens com preço, desconto, prioridade e toggle de comprado

### Planejado 📌
- **Grupos de gastos** — Compartilhar despesas entre usuários
- **Módulo de Investimentos** — Controle de aportes e carteira
- **Integração Wishlist → Dashboard** — Itens comprados refletidos nas transações

---

## 🛠️ Stack Tecnológica

### Back-End
| Tecnologia | Versão | Função |
|------------|--------|--------|
| Java | 21 LTS | Linguagem principal — Records, Pattern Matching |
| Spring Boot | 4.0.3 | Framework principal — IoC, DI, auto-configuração |
| Spring Security | 7.x | Autenticação e autorização |
| Spring Data JPA | 3.x | Abstração do banco via Hibernate |
| JWT (jjwt) | 0.12.6 | Geração e validação de tokens |
| Lombok | 1.18.x | Redução de boilerplate |
| Bean Validation | 3.x | Validação declarativa dos DTOs |
| Springdoc OpenAPI | 3.0.2 | Geração automática do Swagger UI |
| BCrypt | — | Hash seguro de senhas |

### Banco de Dados
| Tecnologia | Uso |
|------------|-----|
| PostgreSQL 16 | Produção (via Docker) |
| H2 in-memory | Desenvolvimento local e testes |

### Front-End *(em desenvolvimento)*
| Tecnologia | Versão | Função |
|------------|--------|--------|
| React | 18 | Biblioteca de UI |
| TypeScript | 5.x | Tipagem estática |
| Vite | 5.x | Build tool com HMR |
| TailwindCSS | 3.4 | Utility-first CSS |
| TanStack Query | 5.x | Cache e sincronização de estado |
| Zustand | 4.x | Gerenciamento de estado global |
| React Hook Form + Zod | — | Formulários com validação |

### Infraestrutura
| Tecnologia | Função |
|------------|--------|
| Docker | Containerização |
| Docker Compose | Orquestração local (backend + postgres) |
| Volumes persistentes | Dados do PostgreSQL sobrevivem a reinicializações |

---

## 🏗️ Arquitetura

O back-end segue o padrão **Layered Architecture** (Arquitetura em Camadas):

```
HTTP Request
     ↓
JwtAuthenticationFilter   → Valida o token JWT em toda requisição
     ↓
Controller                → Recebe o request, valida o DTO com @Valid
     ↓
Service                   → Toda a lógica de negócio
     ↓
Repository                → Abstração do banco (Spring Data JPA)
     ↓
Entity / Database         → Tabelas PostgreSQL / H2
```

### Estrutura de Pacotes

```
com.controledegastos.backend
├── auth/
│   ├── AuthController.java
│   ├── AuthService.java
│   └── dto/
│       ├── RegisterRequestDTO.java
│       ├── LoginRequestDTO.java
│       └── AuthResponseDTO.java
├── config/
│   └── SecurityConfig.java
├── security/
│   ├── JwtService.java
│   └── JwtAuthenticationFilter.java
├── transaction/
│   ├── Transaction.java
│   ├── TransactionController.java
│   ├── TransactionService.java
│   ├── TransactionRepository.java
│   └── dto/
│       ├── TransactionRequestDTO.java
│       └── TransactionResponseDTO.java
├── user/
│   ├── User.java
│   └── UserRepository.java
└── wishlist/
    └── WishlistItem.java
```

---

## 🔒 Segurança

| Mecanismo | Proteção |
|-----------|----------|
| BCrypt (custo 10) | Senhas nunca armazenadas em texto puro |
| JWT Access Token (15min) | Curta duração — minimiza impacto de vazamento |
| JWT Refresh Token (7 dias) | Renovação silenciosa sem novo login |
| Spring Security Filters | Todas as rotas protegidas por padrão |
| Roles (USER/ADMIN) | Controle de acesso baseado em papéis |
| CORS configurado | Apenas origens autorizadas |
| Rate Limiting | Proteção contra força bruta *(planejado)* |
| Bean Validation | Validação de toda entrada na API |
| findByIdAndUser | Usuário só acessa seus próprios dados |

---

## 📡 API Endpoints

### Auth (público)
```
POST /api/auth/register    → Cadastra novo usuário e retorna tokens
POST /api/auth/login       → Autentica e retorna tokens
```

### Transactions (requer Bearer Token)
```
POST   /api/transactions           → Cria nova transação
GET    /api/transactions           → Lista com filtros opcionais
GET    /api/transactions?type=DESPESA&category=ALIMENTACAO
PUT    /api/transactions/{id}      → Atualiza transação
DELETE /api/transactions/{id}      → Remove transação
```

### Dashboard *(em desenvolvimento)*
```
GET /api/dashboard/summary         → Saldo, receitas, despesas e últimas 5 transações
```

### Reports *(em desenvolvimento)*
```
GET /api/reports/monthly?month=2026-03   → Análise mensal com % vs mês anterior
```

### Wishlist *(em desenvolvimento)*
```
POST   /api/wishlist               → Adiciona item
GET    /api/wishlist               → Lista itens
PUT    /api/wishlist/{id}          → Edita item
DELETE /api/wishlist/{id}          → Remove item
PATCH  /api/wishlist/{id}/buy      → Marca como comprado (reversível)
```

---

## 🚀 Como Executar

### Pré-requisitos
- Java 21+
- Maven 3.9+
- Docker + Docker Compose

### 1. Clone o repositório
```bash
git clone https://github.com/Guilhermecorral/Controle-de-Gastos.git
cd Controle-de-Gastos
```

### 2. Configure as variáveis de ambiente

Crie o arquivo `.env.properties` dentro de `backend/back/`:

```properties
# PostgreSQL (Docker)
POSTGRES_USER=seu_usuario
POSTGRES_PASSWORD=sua_senha
SPRING_DATASOURCE_USERNAME=seu_usuario
SPRING_DATASOURCE_PASSWORD=sua_senha

# JWT — use uma chave longa e aleatória em produção
JWT_SECRET=sua-chave-secreta-minimo-32-caracteres-aqui
JWT_EXPIRATION=900000
JWT_REFRESH_EXPIRATION=604800000
```

> ⚠️ **Nunca commite o `.env.properties`** — ele já está no `.gitignore`.

### 3. Suba o banco de dados

```bash
# Na raiz do projeto
docker compose up -d postgres
```

### 4. Execute o back-end

**Pelo IntelliJ:** Abra `BackendApplication.java` e clique no ▶ verde.

**Pelo terminal:**
```bash
cd backend/back
mvn spring-boot:run
```

### 5. Acesse a documentação da API

```
http://localhost:8080/swagger-ui.html
```

### 6. Execute via Docker Compose (opcional)

```bash
# Sobe tudo: banco + back-end
docker compose up -d
```

API disponível em `http://localhost:8081`

---

## 🧪 Testes

```bash
cd backend/back

# Roda todos os testes (usa H2 in-memory — não precisa do Docker)
mvn test

# Build completo com testes
mvn clean install
```

Os testes usam o perfil `test` com H2 in-memory, garantindo que rodam em qualquer máquina sem dependências externas.

---

## 🗄️ Modelo de Dados

```
users
├── id (PK)
├── name
├── email (UNIQUE)
├── password (BCrypt hash)
├── role (USER | ADMIN)
└── created_at

transactions
├── id (PK)
├── user_id (FK → users)
├── type (RECEITA | DESPESA)
├── description
├── category (ALIMENTACAO | TRANSPORTE | MORADIA | SAUDE | LAZER | EDUCACAO | COMPRAS | OUTROS)
├── amount (DECIMAL 19,2)
├── payment_method (PIX | CARTAO_DEBITO | CARTAO_CREDITO_AVISTA | CARTAO_CREDITO_PARCELADO | DINHEIRO)
├── transaction_date
└── created_at

wishlist_items
├── id (PK)
├── user_id (FK → users)
├── description
├── original_price
├── discount_percent
├── final_price
├── priority (ALTA | MEDIA | BAIXA)
├── category
├── status (PENDENTE | COMPRADO)
├── notes
├── bought_at
└── created_at
```

---

## 📁 Variáveis de Ambiente

| Variável | Descrição | Exemplo |
|----------|-----------|---------|
| `POSTGRES_USER` | Usuário do PostgreSQL | `admin` |
| `POSTGRES_PASSWORD` | Senha do PostgreSQL | `senha123` |
| `SPRING_DATASOURCE_USERNAME` | Usuário do datasource Spring | `admin` |
| `SPRING_DATASOURCE_PASSWORD` | Senha do datasource Spring | `senha123` |
| `JWT_SECRET` | Chave secreta JWT (min. 32 chars) | `minha-chave-secreta-...` |
| `JWT_EXPIRATION` | Expiração do Access Token (ms) | `900000` (15 min) |
| `JWT_REFRESH_EXPIRATION` | Expiração do Refresh Token (ms) | `604800000` (7 dias) |

---

## 🗺️ Roadmap

- [x] Configuração inicial do projeto (Spring Boot + Docker + PostgreSQL)
- [x] Entidades JPA (User, Transaction, WishlistItem)
- [x] Autenticação completa (Register, Login, JWT, Spring Security)
- [x] Transações CRUD com filtros e isolamento por usuário
- [ ] Dashboard (saldo, receitas, despesas, últimas transações)
- [ ] Análise Mensal com comparativo percentual
- [ ] Wishlist com toggle de comprado reversível
- [ ] Front-End React + TypeScript
- [ ] Grupos de gastos compartilhados
- [ ] Módulo de Investimentos

---

## 👤 Autor

**Guilherme Corral**

[![GitHub](https://img.shields.io/badge/GitHub-Guilhermecorral-181717?style=flat-square&logo=github)](https://github.com/Guilhermecorral)

---

## 📄 Licença

Este projeto está sob a licença MIT. Veja o arquivo [LICENSE](LICENSE) para mais detalhes.

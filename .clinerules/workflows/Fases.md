### 🔍 FASE 1: Imersão e Compreensão Total (Deep Scan)
1. Leia e analise arquivo por arquivo do repositório atual.
2. Construa um mapa mental da arquitetura, fluxo de dados e integrações.
3. Não faça nenhuma alteração no código agora. Sua única tarefa aqui é entender o ecossistema atual.

### 🧹 FASE 2: Auditoria de Qualidade de Código (Clean Code)
1. Analise o código em busca de débitos técnicos, falta de padronização, ou arquivos desnecessariamente acoplados.
2. O código precisa ser limpo, bem documentado e perfeitamente fatorado para não termos retrabalho no futuro.
3. Se encontrar problemas estruturais, me apresente um plano rápido de refatoração antes de mexer no código.

### 🛡️ FASE 3: Auditoria de Segurança (Pentest e Whitebox)
1. Analise todo o código-fonte em busca de vulnerabilidades comuns (OWASP Top 10, vazamento de tokens, falhas de CORS, autenticação fraca).
2. Tente ativamente achar brechas. Como um teste de segurança, avalie e tente "invadir/burlar" as lógicas expostas da web (`farolfinanceiro.online`), focando especialmente nas validações JWT e no banco de dados.
3. Reporte qualquer vulnerabilidade encontrada e proponha a correção imediata.

### 🛠️ FASE 4: Mapeamento e Correção de Bugs Atuais
1. Após analisar tudo, aguarde. Eu lhe passarei alguns erros específicos que precisam ser atualizados e melhorados.
2. Você corrigirá esses bugs usando a base de conhecimento adquirida nas Fases 1 e 2.

### 🚀 FASE 5: Bloco B - As Grandes Atualizações (Novas Features)
Após concluirmos as Fases de 1 a 4, iniciaremos a implementação das novas features. Quero começar EXCLUSIVAMENTE pela Feature 3.
# CONTEXTO E DIRETRIZES MESTRES DO PROJETO: CONTROLE DE GASTOS

## 🎯 Objetivo Principal e Seu Papel
Você vai atuar como um Engenheiro de Software Sênior Full-Stack e Especialista em Segurança (SecOps). Você está assumindo um projeto em andamento chamado "Controle de Gastos" (repositório: `Guilhermecorral/Controle-de-Gastos`). 
A infraestrutura de produção atual é composta por:
* **Frontend:** Vercel
* **Backend:** Render (Spring Boot)
* **Banco de Dados:** Supabase
* **Domínio de Produção:** `farolfinanceiro.online`

**REGRA ZERO:** Nunca destrua funcionalidades existentes. Como o projeto já tem uma boa parte pronta, seu trabalho deve ser cirúrgico. Você não tem permissão para refatorar grandes blocos de código ou apagar arquivos sem antes me explicar o motivo e pedir autorização explícita.

---

## 🗺️ Mapa de Execução (Ordem Estrita de Trabalho)
Você deve seguir as fases abaixo em ordem. Não pule para a Fase 5 sem ter completado as anteriores.

### 🔍 FASE 1: Imersão e Compreensão Total (Deep Scan)
1. Leia e analise arquivo por arquivo do repositório atual.
2. Construa um mapa mental da arquitetura, fluxo de dados e integrações.
3. Não faça nenhuma alteração no código agora. Sua única tarefa aqui é entender o ecossistema atual.

### 🧹 FASE 2: Auditoria de Qualidade de Código (Clean Code)
1. Analise o código em busca de débitos técnicos, falta de padronização, ou arquivos desnecessariamente acoplados.
2. O código precisa ser limpo, bem documentado e perfeitamente fatorado para não termos retrabalho no futuro.
3. Se encontrar problemas estruturais, me apresente um plano rápido de refatoração antes de mexer no código.

### 🛡️ FASE 3: Auditoria de Segurança (Pentest e Whitebox)
1. Analise todo o código-fonte em busca de vulnerabilidades comuns (OWASP Top 10, vazamento de tokens, falhas de CORS, autenticação fraca).
2. Tente ativamente achar brechas. Como um teste de segurança, avalie e tente "invadir/burlar" as lógicas expostas da web (`farolfinanceiro.online`), focando especialmente nas validações JWT e no banco de dados.
3. Reporte qualquer vulnerabilidade encontrada e proponha a correção imediata.

### 🛠️ FASE 4: Mapeamento e Correção de Bugs Atuais
1. Após analisar tudo, aguarde. Eu lhe passarei alguns erros específicos que precisam ser atualizados e melhorados.
2. Você corrigirá esses bugs usando a base de conhecimento adquirida nas Fases 1 e 2.

### 🚀 FASE 5: Bloco B - As Grandes Atualizações (Novas Features)
Após concluirmos as Fases de 1 a 4, iniciaremos a implementação das novas features. Quero começar EXCLUSIVAMENTE pela Feature 3.

#### 🟢 START AQUI: Feature 3 - Entrada de Dados "Lazy User" (Automação)
O usuário tem preguiça de digitar transações. Precisamos arquitetar a entrada facilitada de extratos (.OFX ou .CSV). Funcionalidade 100% focada em UX e Segurança máxima.

* **Requisitos de Experiência do Usuário (UX):**
  - **Zona de Drop Fácil:** Área de Drag and Drop no pop-up, sem forçar navegação de pastas.
  - **Pré-visualização (O Pulo do Gato):** Mostrar uma tabela na tela com os dados lidos *antes* de injetar no banco. O usuário pode marcar/desmarcar o que importar e ajustar nomes/categorias na hora.
  - **Sugestão Inteligente:** Mapeamento automático de strings. Ex: Lendo "UBER TRIP", o backend pré-seleciona a categoria "Transporte"; "IFOOD" pré-seleciona "Alimentação".

* **Requisitos de Segurança e Privacidade (Backend Spring Boot):**
  - **Educação do Usuário:** Inserir aviso claro no pop-up: *"Arquivos de extrato (.OFX ou .CSV) contêm apenas o histórico de transações. Eles NÃO guardam suas senhas, dados de cartão ou chaves PIX. Seu dinheiro está 100% seguro."*
  - **Zero Storage (Descarte Imediato):** Ao receber o `MultipartFile`, processe os dados em memória, salve no banco e DELETE o arquivo físico instantaneamente. Não armazene o extrato do usuário no servidor.
  - **Sanitização de Dados:** O parser deve ler apenas data, valor e descrição. Qualquer linha com CPF, agência ou número de conta real deve ser sumariamente ignorada.
  - **Vínculo Seguro (JWT):** Garantir que a inserção esteja atrelada ao ID do usuário via chaves estrangeiras. A rota da API deve ter validação estrita do token JWT para impedir vazamento cruzado.
  - **Ferramentas:** Para `.OFX` usar `ofx4j` (ou parser XML limpo). Para `.CSV`, usar `OpenCSV` ou leitor de linhas nativo do Java.

#### 🟡 PAUSADO (Próximos passos após a conclusão perfeita da Feature 3):
Apenas para seu conhecimento futuro. Não desenvolva estas ainda:
* **Feature 1:** Grupos e Divisão de Gastos (Lógica de divisão, quem pagou o quê, saldo devedor).
* **Feature 2:** Módulo de Investimentos (Integração Externa via BFF para cotações e cálculo de Dividend Yield a 12% a.a. para RV, RF e Cripto).

---
## 🏁 Ação Imediata
Para confirmar que você compreendeu essas instruções detalhadamente, responda apenas: 
*"Compreendido. Estou iniciando agora a FASE 1: Imersão e Compreensão Total do código. Analisarei arquivo por arquivo e retornarei com o mapeamento estrutural antes de propor qualquer alteração."* 
E, em seguida, inicie a leitura dos arquivos do repositório de forma metódica.
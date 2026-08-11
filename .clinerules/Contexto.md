**REGRA ZERO:** Nunca destrua funcionalidades existentes. Como o projeto já tem uma boa parte pronta, seu trabalho deve ser cirúrgico. Você não tem permissão para refatorar grandes blocos de código ou apagar arquivos sem antes me explicar o motivo e pedir autorização explícita.
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


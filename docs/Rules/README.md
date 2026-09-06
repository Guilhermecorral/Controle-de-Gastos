# Regras de Contribuição

Este guia orienta pessoas e agentes que alteram o Farol Financeiro. Ele existe para preservar coerência de produto, segurança e qualidade sem substituir julgamento técnico. Quando uma regra não se aplicar a um caso concreto, registre a exceção, a razão e o impacto antes de seguir.

## Como usar

Antes de iniciar uma alteração relevante:

1. Leia este guia e o [roadmap do produto](../ROADMAP.md).
2. Consulte a documentação específica da área: [arquitetura](../ARCHITECTURE.md), [API](../API.md), [investimentos](../INVESTMENTS-1.4.md) e [desenvolvimento](../DEVELOPMENT.md).
3. Confirme o comportamento atual no código, nos testes e, quando necessário, no ambiente correto antes de assumir a causa de um problema.
4. Mantenha a alteração pequena e reversível sempre que isso não prejudicar a solução.

Para agentes automatizados, estas regras são contexto de projeto e devem ser aplicadas dentro das políticas e permissões do ambiente em uso.

## Princípios do produto

- O Farol deve resolver um problema melhor do que uma planilha: automação confiável, correção fácil de erros, integração entre áreas e explicação clara.
- O usuário controla o dinheiro real. Previsões, importações e proventos não podem alterar saldo sem uma confirmação apropriada.
- Linguagem de interface é orientada ao usuário. Evite expor jargão de backend, siglas fiscais sem explicação ou mensagens genéricas como "erro inesperado" quando houver orientação mais útil.
- Dados financeiros, tributários e de mercado são informativos quando a fonte ou a regra não for definitiva. Sempre expor fonte, data de atualização e limitações relevantes.
- Recursos incompletos devem ser identificados como tal. Não apresentar mock, estimativa ou dado atrasado como informação em tempo real.

## Versionamento e release

Uma alteração de funcionalidade não exige nova versão automaticamente. Quando uma entrega for definida como release, sincronize a versão em todos os locais aplicáveis:

- badge e frase de desenvolvimento no `README.md` da raiz;
- bloco com data e itens reais no `CHANGELOG.md`;
- `frontend/package.json`;
- `pom.xml` do backend, se houver versão própria;
- versão apresentada em runtime, rodapé, configurações ou `build-info.json`;
- documentos que mencionam a versão atual, incluindo `docs/ARCHITECTURE.md`, `docs/API.md`, `docs/INVESTMENTS-1.4.md` e o resumo do projeto.

Nunca criar uma entrada de changelog para trabalho planejado, nem atualizar número de versão sem entrega correspondente. Se uma alteração explicitamente não muda a versão, declare isso no encerramento da tarefa.

## Experiência de uso

Use as heurísticas de Nielsen como checklist, não como decoração burocrática:

- Mostrar carregamento, sucesso, falha e progresso de operações demoradas.
- Preferir conceitos familiares ao usuário e explicar regras financeiras no ponto da tarefa.
- Oferecer cancelar, voltar ou desfazer quando a ação permitir; operações destrutivas exigem confirmação com impacto explicado.
- Reutilizar componentes, texto, cor e posição para a mesma ação em todo o sistema.
- Validar antes do envio e impedir duplicidade ou dados impossíveis sem bloquear correções legítimas.
- Exibir contexto e resumos para reduzir a necessidade de memorizar informações entre telas.
- Manter telas limpas e priorizar a informação decisiva; detalhes avançados podem ficar em accordion, ajuda contextual ou fluxo secundário.
- Escrever erros acionáveis: o que falhou, por que importa e como resolver.

Interface deve ser verificada em desktop e mobile. Para mudanças visuais relevantes, comparar com componentes e gráficos existentes antes de criar um estilo paralelo.

## Engenharia e arquitetura

- Nomes de código devem ser claros, consistentes com a linguagem do projeto e revelar intenção. Comentários explicam decisões ou riscos, não o óbvio.
- Não duplicar regra de negócio entre telas, controllers ou serviços. Extrair uma abstração somente quando houver uma responsabilidade comum real.
- Backend segue separação entre controller, service, repository, DTO e entidade. Controllers validam e orquestram; regras financeiras e fiscais pertencem ao backend.
- Evitar consultas N+1, efeitos colaterais ocultos e transações parciais em operações que alteram carteira e fluxo financeiro juntos.
- Frontend reutiliza o design system e a camada de consultas existente. Otimizações como memoização, virtualização ou estado global adicional devem ser usadas quando houver necessidade observável, não por padrão.
- Contratos de API devem evoluir com compatibilidade ou migração explícita. Mudanças de banco exigem nova migração Flyway; migrações aplicadas não são reescritas.
- Novas integrações externas precisam de interface, timeout, fallback controlado, cache e indicação de fonte/horário na resposta.

## Dados financeiros, importação e investimentos

- `INVESTIMENTO` permanece uma categoria sobre `RECEITA` e `DESPESA`; não criar terceiro tipo de transação sem uma decisão de produto e migração clara.
- Compra, venda, aplicação, resgate, provento e imposto vinculado precisam preservar rastreabilidade. Correção controlada é preferível a apagar fatos históricos.
- Dados importados entram primeiro em staging e só se tornam movimentações após revisão e confirmação do usuário.
- Possíveis duplicidades devem ser sinalizadas, nunca silenciosamente ignoradas ou gravadas.
- Proventos são previsão até a conciliação manual. Em especial, JCP deve mostrar bruto, retenção e líquido.
- Cotações e eventos corporativos devem manter fonte, horário e escopo de cobertura. Falha de provedor não pode apagar a última informação válida nem bloquear o acesso à carteira.
- Cálculos tributários precisam declarar que são estimativas quando houver dependência de regime, documento, data ou informação que o sistema não conhece.

## Segurança e privacidade

- Nunca gravar em Git segredos, tokens, senhas, chaves de API, arquivos `.env`, bancos locais ou documentos de usuários.
- Usar variáveis de ambiente e manter exemplos sem valores reais.
- Não expor stack trace, token, senha, conteúdo integral de comprovante ou dado pessoal em resposta de API, frontend ou logs.
- Autorização é responsabilidade do backend; esconder uma ação no frontend não substitui controle de acesso.
- Em alterações de autenticação, validar ciclo completo de cookie, refresh token, expiração, revogação, proxy e persistência no banco.
- Antes de integrar fonte externa ou fazer coleta automatizada, verificar termos de uso, limites e adequação do dado. Não contornar bloqueios, CAPTCHA ou controles anti-automação.

## Qualidade e validação

- Executar os testes e builds proporcionais à alteração. Para mudanças no backend, preferir `./mvnw test`; para frontend, `npm run build` e os testes disponíveis.
- Adicionar ou atualizar testes para regra de negócio nova, correção de regressão ou borda relevante.
- Quando não for possível executar uma validação, declarar claramente o motivo e o risco residual.
- Revisar o diff antes de concluir: não misturar formatação ampla, arquivos gerados ou mudanças sem relação com a tarefa.
- Nunca reverter alterações preexistentes de outra pessoa sem solicitação explícita.

## Documentação e comunicação

- Atualizar documentação quando comportamento, contrato, limitação ou operação do sistema mudar.
- Registrar no [roadmap](../ROADMAP.md) decisões que afetem versões futuras, fontes externas, riscos de produto ou dependências de terceiros.
- Encerramentos devem informar o que mudou, validações executadas, limitações e decisão de versão. Commit, push e deploy exigem autorização explícita quando essa for a regra da tarefa.

## Exceções conscientes

Uma exceção é aceitável quando melhora segurança, correção ou simplicidade. Ela deve responder, de forma curta:

1. Qual regra não foi seguida?
2. Por que ela não se aplica neste caso?
3. Qual risco foi aceito e como sera acompanhado?

O objetivo destas regras não é limitar iniciativa. É garantir que velocidade de entrega não sacrifique confiança financeira, clareza para o usuário ou sustentabilidade do código.

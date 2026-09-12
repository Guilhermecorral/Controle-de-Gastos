# Changelog

As mudanças relevantes do Farol Financeiro seguem versionamento semântico.

## [1.4.5-beta.1] - 2026-09-06

### Entrega inicial

- A Agenda de Proventos B3 virou um piloto controlado: somente administradores e e-mails autorizados por ambiente podem consultar a prévia e publicar previsões revisáveis.
- A consulta B3 não cria receita, transação ou saldo; somente a confirmação explícita de recebimento gera a receita `INVESTIMENTO` vinculada.
- Eventos de uma classe explicitamente diferente da posição, como uma PN retornada para uma carteira que possui apenas BBDC3, são descartados antes da prévia; `AMBIGUO` fica reservado para respostas em que a B3 realmente não identifica a classe.
- O adaptador B3 não depende mais de um bean legado do Jackson 2, evitando falha de inicialização no Spring Boot 4 quando o piloto é habilitado.
- A prévia B3 passa a informar a fonte realmente ativa e o backend registra, por atualização manual, quantos eventos foram lidos e quantos ficaram elegíveis; isso permite diagnosticar configuração ou resposta da fonte sem registrar dados pessoais.
- Eventos B3 sem ticker resolvido não interrompem mais a prévia: eles são mantidos como itens para revisão, enquanto os ativos vinculados com segurança continuam aparecendo normalmente.
- Atualizações repetidas da Agenda reconhecem previsões pelo identificador da fonte ou pela combinação de ticker, Data Com e pagamento, inclusive quando foram criadas manualmente ou por mocks anteriores. Previsões canceladas continuam identificáveis e podem ser restauradas explicitamente, sem voltarem como novas ou criarem duplicidade.
- A prévia diferencia uma pendência de vínculo ambíguo de uma posição inelegível na Data Com, que é apenas informação diagnóstica; eventos econômicos idênticos devolvidos pela B3 são deduplicados antes de exibir ou persistir.
- A compra e a importação assistida passam a reconhecer FIAGRO separadamente de FII; a busca BRAPI consulta o subtipo `fi-agro` e possui catálogo de contingência para os principais tickers.
- FIAGRO passa a existir como tipo visual separado de FII, sem herdar regras tributárias automaticamente.
- A migração V19 libera a persistência de FIAGRO na carteira; antes, o banco rejeitava compras como RURA11 apesar de a busca já identificar o tipo corretamente.
- A Agenda agora mostra somente proventos que ainda exigem ação; recebidos, efetivados e cancelados ficam no Histórico Financeiro e podem ser consultados por um histórico opcional na própria Agenda.
- A prévia B3 tolera até dois minutos de inicialização e consulta, evitando que o despertar do Render seja mostrado como falha da Agenda.
- O simulador de renda fixa mostra a tabela completa mês a mês, usa cinco marcos apenas no gráfico e calcula poder de compra real a partir da inflação anual informada.
- Confirmações de exclusão, ajustes fiscais e ações administrativas passam a usar diálogos internos do Farol, com impacto, cancelamento, validação e erro visíveis, sem depender de alertas ou prompts nativos do navegador.
- O painel administrativo permite zerar dados de homologação de uma conta: transações, comprovantes, wishlist, investimentos, proventos, metas, saldos fiscais e staging são removidos em lote, enquanto usuário, senha, papel, status, sessão e 2FA permanecem preservados.
- ETFs passam a consultar o catálogo BRAPI e, quando a busca não traz preço, o modal de compra consulta a cotação do ativo sem sobrescrever uma correção manual. Criptoativos preservam quantidades fracionárias e preço em BRL com até oito casas decimais, em cache por cinco minutos.
- A Agenda permite selecionar proventos com pagamento já conciliável e confirmar os recebimentos em lote mediante confirmação explícita; cada item cria sua receita vinculada e continua reversível pelo histórico.

### Continuação auditada por commit

- `5a1bbed` (2026-09-07) tornou a cobertura da fonte auditável por ativo, quantidade de eventos e intervalo de Data Com; a elegibilidade passou a expor a primeira compra registrada. A Agenda e o Histórico foram separados, com filtro por estado, seleção dos itens visíveis e carregamento progressivo da lista.
- `00aa231` (2026-09-07) eliminou séries técnicas de fundos pelo ISIN canônico, ampliou a consulta de proventos para todo o período conhecido da posição e adicionou ações em lote para cancelar, restaurar ou desfazer recebimentos sem aceitar IDs repetidos. Compras fracionadas de cripto passaram a preservar quantidade e precisão em vez de aplicar regras de lotes de ações.
- `3bd0464` (2026-09-08) tornou cancelamento um estado reversível, separou ambiguidade de inelegibilidade na Data Com e deduplicou eventos econômicos antes da prévia e persistência. Também adicionou o workflow geral de CI, configuração de lint e o manifesto versionado do `COTAHIST`.
- `9918c1a` (2026-09-08) bloqueou o piloto quando o provedor ativo ainda é `mock`, documentou a ativação manual segura e manteve cron desligado. Corrigiu o reset de senha para usar somente a URL de frontend configurada e adicionou o workflow de segurança com Gitleaks e OpenGrep.
- `e52e714` (2026-09-09) distinguiu os estados novo, provisionado, pendente de confirmação, confirmado, cancelado, inelegível e ambíguo; criou `InstrumentCatalog` e `AssetResolver`; cadastrou BDR/ETF sem Agenda automática; e adicionou histórico B3 paginado com cache persistido, deduplicação e filtro por ISIN. A prova de conceito CVM de `MXRF11`/`RURA11` e os contratos separados de dados de mercado permanecem sem automação de Agenda.
- O mapeamento JPA do payload do cache B3 passou a usar `TEXT`, igual à migration V21 e ao PostgreSQL de produção, evitando que o Hibernate espere um `oid` e interrompa o deploy durante a criação do `entityManagerFactory`.

## [1.4.4] - 2026-09-06

- Compra e venda de investimentos passam a invalidar somente Carteira, Transações, Painel e Análise Mensal, removendo a atualização global duplicada que podia deixar o modal sem resposta após uma venda.
- O diálogo de movimentação é desmontado ao fechar, abre acima de outros overlays e encerra diálogos concorrentes para impedir estado residual ou uma camada invisível de bloquear Compra, Venda, Renda Fixa, Trocar e Fechar.
- Cada abertura de Nova movimentação agora cria uma sessão isolada e o botão Fechar descarta o próprio diálogo imediatamente, sem depender do ciclo de atualização da tela de Investimentos.
- Compra retroativa de BBAS3 foi coberta junto da Agenda: quando a operação é anterior à Data Com, ela permanece salva, gera despesa de investimento e participa da quantidade elegível.
- Agenda vazia explica que a fonte pode estar no modo demonstrativo e que operações posteriores à Data Com não têm direito ao evento já anunciado.
- O fluxo de renovação silenciosa continua coberto para access token ausente/expirado no navegador e refresh token persistido válido.

## [1.4.3] - 2026-09-06

- GET /api/auth/me agora devolve 401 quando não há sessão de acesso válida, permitindo que o frontend use o refresh token persistido antes de declarar logout após um F5.
- A importação assistida de investimentos também está disponível no Histórico Financeiro e abre o mesmo lote de staging, revisão de duplicidades e confirmação explícita da Carteira.
- Novo provedor B3 experimental, ativado somente por configuração, consulta em série os ativos já presentes na carteira e preserva Data Com e data de pagamento sem criar saldo automaticamente.
- PDFs SINACOR nativos entram apenas como prévia revisável: operações, custos e IRRF podem ser ajustados antes de qualquer confirmação; OCR e PDFs escaneados permanecem fora do suporte.
- Testes de sessão, parser B3 para ações/FIIs/Fiagros e staging de PDF nativo adicionados; referências de versão sincronizadas para 1.4.3.

## [1.4.2] - 2026-09-05

- Refresh tokens agora são persistidos e rotacionados no PostgreSQL, com revogação no logout e validade padrão de 30 dias; a assinatura continua dependente de `JWT_SECRET` fixo no ambiente.
- Nova importação assistida de investimentos para CSV, XLS, XLSX e OFX de investimentos: o arquivo é salvo apenas como lote de revisão, aponta possíveis duplicidades e não cria movimentações antes da confirmação.
- A confirmação do lote reutiliza o fluxo oficial de compra e venda, preservando preço médio, transações `INVESTIMENTO`, painel e análise mensal.
- PDF B3 possui apenas ponto de extensão protegido por feature flag; não há parser ou endpoint em produção nesta versão.
- Migrações V16/V17, contratos, interface de revisão e referências de versão atualizados para `1.4.2`.

## [1.4.1] - 2026-09-05

- Compras e vendas retroativas passam a recalcular cronologicamente a posição, o custo médio, o resultado de vendas posteriores e os lançamentos financeiros vinculados.
- Agenda automática de proventos com eventos corporativos, snapshot de elegibilidade na Data Com e estados provisionado, a confirmar, efetivado e cancelado.
- Provedor mock isolado por interface para PETR4 e BBAS3; JCP exibe IRRF de 15% no fluxo da aplicação.
- A confirmação do recebimento cria a receita `INVESTIMENTO`; previsões não alteram o saldo financeiro.
- Migração V15, contratos, interface da agenda e referências de versão atualizados para `1.4.1`.

## [1.4.0] - 2026-09-05

- Release oficial da base de investimentos com lançamentos financeiros vinculados, correções controladas e renda fixa com liquidez diária.
- Simulador de renda fixa consolidado com projeção bruta, IR/IOF estimados, tabela por período e gráfico alinhado ao padrão visual de Evolução Patrimonial.
- Fechamento mensal guiado, estados tributários explícitos e pagamento de DARF separado da estimativa mantidos como recursos da release.
- Referências de runtime, pacote frontend, backend e documentação sincronizadas para `1.4.0`.

## [1.4.0-beta.2] - 2026-09-05

- Nova movimentação separada visualmente em renda variável e renda fixa, com compra/venda, busca de ativo e importação de saldo inicial fora do fluxo recorrente.
- Renda fixa com rentabilidade prefixada, pós-fixada ou híbrida, indexador e opção de liquidez diária sem vencimento obrigatório.
- Correção controlada de compras e vendas: edição recalcula quantidade, preço médio, resultado realizado e fluxo financeiro vinculado; exclusão exige confirmação de impacto.
- Eventos fiscais passam a diferenciar imposto retido integralmente, IRRF antecipado, possível DARF e isenção, com ajuste manual de retenção baseado no comprovante.
- Fechamento mensal reescrito em linguagem simples e explicação embutida sobre retenção na fonte, IRRF e DARF.
- Simulador de renda fixa mantém a tabela por período e passa a exibir gráfico de linha da projeção de saldo.
- Contratos, migrações V13/V14, documentação e referências de versão atualizados para esta prévia.

## [1.4.0-beta.1] - Em desenvolvimento

- Compras, vendas, aplicacoes e resgates geram fluxo financeiro vinculado, separado dos indicadores de consumo.
- Tipo visual Investimento nas transacoes e custos operacionais detalhados, com cambio informado para ativos estrangeiros.
- Saldos iniciais de carteira sem movimentacao ficticia de caixa.
- Simulador e resgate total de renda fixa com estimativas de IR/IOF por prazo e por aporte.
- Apuracao inicial de acoes comuns e FIIs, saldos tributarios anteriores e pagamento de DARF separado da estimativa.
- Revisao obrigatoria para operacoes ainda nao suportadas ou divergencias de pagamento.
- Escopo e pendencias documentados em [Investimentos 1.4](docs/INVESTMENTS-1.4.md). Esta previa nao conclui toda a v1.4.

## [1.3.4] - 2026-09-03

- Painel de Tributação e Conciliação com impostos retidos, proventos sem retenção informada e vendas que exigem apuração.
- Conciliação de compras, vendas e proventos em reais com lançamentos de extratos OFX, CSV, TSV e Excel já importados.
- Importador de extratos acessível pela carteira, com revisão das linhas antes da gravação e atualização da conciliação.
- Estados explícitos para itens conciliados, gerados pelo Farol, pendentes e que exigem revisão.

## [1.3.3] - 2026-09-03

- Agenda de proventos por ativo com valor por cota, Data Com opcional, pagamento e status de recebimento.
- Estimativa bruta, imposto retido e valor líquido calculados pela quantidade existente na Data Com.
- Confirmação de recebimento integra o valor líquido ao histórico de investimentos e às receitas financeiras, sem duplicidade.
- Metas de patrimônio com objetivo, aporte mensal, variação anual opcional, progresso e previsão de conclusão.
- Metas passam a usar valor inicial e aportes exclusivos, com edição posterior da taxa e do planejamento.
- Histórico de aportes por meta com remoção individual para correções de lançamento.

## [1.3.2] - 2026-09-02

- Painel individual do ativo com preço médio, cotação atual, ganho de capital, proventos e retorno total separados.
- Rentabilidade consolidada corrigida para distinguir valorização da posição e renda efetivamente registrada.
- Evolução patrimonial baseada em retratos diários reais, sem fabricar cotações anteriores ao início do acompanhamento.
- Gráfico patrimonial reformulado com escala monetária, capital investido, valor da carteira e variação do período.
- Simulador de juros compostos com valor inicial, aporte mensal, taxa mensal ou anual e visão mensal ou anual.
- Resultado do simulador com saldo final, total investido, juros ganhos, composição visual e tabela por período.
- Registro de proventos redesenhado como modal integrado à identidade visual do Farol, com valor e data de recebimento.

## [1.3.1] - 2026-09-02

- Busca inteligente de ações brasileiras e americanas, FIIs e criptoativos por ticker ou nome, sem exigir cadastro manual do nome oficial.
- Catálogo com mercado, bolsa, moeda, identificador do provedor e fallback local para ativos populares.
- Registro de compras e vendas com quantidade, preço unitário, custos e data da operação.
- Posições e preço médio recalculados automaticamente, incluindo custos de compra e proteção contra vendas acima da custódia.
- Conversão cambial para consolidar posições internacionais em reais, preservando preços na moeda original.
- Migração compatível das posições variáveis existentes para movimentações iniciais automáticas.
- Nova experiência de carteira com modal de movimentação, histórico recente e origem/horário das cotações.

## [1.3.0] - 2026-09-02

- Painel administrativo recuperável para contas que já possuem role `ADMIN` quando a whitelist de ambiente estiver ausente, sem liberar novas promoções.
- Carteira de investimentos por usuário para ações, FIIs, criptoativos e renda fixa.
- BFF de cotações com cache, timeout, Brapi opcional, fallback Yahoo Finance e CoinGecko para cripto.
- Simulador de renda fixa com juros compostos, taxa padrão de 12% a.a. e aviso de caráter educacional.
- Distribuição da carteira, retorno estimado e registro de dividendos/rendimentos integrado às receitas financeiras.

## [1.1.0] - 2026-08-30

- Importador universal com suporte a OFX, CSV, TSV, XLS e XLSX.
- Detecção de tabelas transacionais, colunas financeiras, matrizes mensais e blocos semiestruturados.
- Compatibilidade validada com os históricos completos de João e Rafael, incluindo arquivos Windows-1252.
- Pré-visualização paginada com origem, confiança, justificativa e alertas de leitura.
- Proteção contra duplicidade de saldos, fluxos, patrimônios, reservas e totais calculados.
- Copiloto financeiro local com dicas explicáveis sobre saldo, comprometimento, crescimento e concentração de gastos.

## [1.0.0] - 2026-08-29

- Primeira versão de produção formalmente identificada.
- Sessão com JWT em cookies `HttpOnly`, autenticação em dois fatores e Turnstile.
- Painel financeiro, transações, análise mensal, wishlist, notas fiscais e administração.
- Importação segura de extratos CSV e OFX com pré-visualização.
- SEO técnico, favicon, sitemap, analytics consentido e metadados de release.
- Proxy same-origin entre Vercel e Render para preservar a sessão em navegação privada.

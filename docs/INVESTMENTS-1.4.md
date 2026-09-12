# Investimentos 1.4

Status: investimentos `1.4.5` homologados; documento atualizado para o código `1.4.6` em 12/09/2026. A base de investimentos mantém Agenda B3 em operação controlada, importação assistida e fontes externas revisáveis.

## Entregue nesta etapa

- Tipo visual Investimento no modal e filtro do historico; persistencia como RECEITA/DESPESA com categoria INVESTIMENTO.
- Compra, venda, aplicacao e resgate geram transacao financeira na mesma transacao de banco. Compras e vendas retroativas são reordenadas para recalcular custo médio, resultado realizado e caixa vinculado.
- Ligacao unica `transactions.investment_movement_id`; edicao/exclusao avulsa do financeiro e bloqueada para preservar o saldo da carteira.
- Custos de corretagem, B3 e outros custos separados do IRRF antecipado. Compras incorporam custos ao preco medio. Vendas guardam custo proporcional e resultado antes de IR.
- Vendas registram como entrada o bruto menos custos e IRRF. IRRF nao e deduzido novamente do lucro tributavel.
- Operacoes em moeda estrangeira exigem cambio informado da operacao para o fluxo em BRL; cotacao atual nao substitui cambio historico.
- Saldo inicial de ativos, com quantidade, custo medio incluindo taxas e data inicial do acompanhamento; sem lancamento de caixa. Renda fixa preexistente preserva a data original da aplicacao.
- Regime de renda fixa regressivo, isento de IR ou aliquota manual. IOF selecionado separadamente, pois isencao de IR nao implica necessariamente isencao de IOF.
- Simulacao com imposto por lote de aporte, datas exatas e periodos parciais. Cada linha mostra a hipotese de resgate naquela data, sem deduzir impostos mensalmente do saldo reinvestido.
- Resgate total de renda fixa com previa de IR, IOF e liquido; confirmacao encerra a posicao e cria receita liquida. Resgates futuros nao podem ser confirmados.
- Saldo tributario inicial com prejuizos comum/day trade/FII, creditos e imposto inferior a R$ 10. Origem e competencia registradas, sem efeito no caixa.
- Apuracao sob demanda de acoes comuns B3 e FIIs em BRL: limite inclusivo de R$ 20 mil para acoes, perdas compensaveis, IRRF comum e acumulacao de DARF inferior a R$ 10.
- Meses incompletos, vendas legadas sem custo realizado, operacoes no mesmo dia, cripto e exterior ficam em revisao; a incerteza e propagada aos meses seguintes.
- Pagamento de DARF 6015/4600 com competencia, vencimento da guia, conta identificada por texto e comprovante identificado por referencia. Apenas a confirmacao gera despesa IMPOSTOS. A conta ainda nao representa um cadastro bancario independente.
- Dashboard e Analise Mensal alertam sobre competencias para revisar/pagar. Aportes saem dos graficos e alertas de consumo, permanecendo nos totais do fluxo de caixa.
- Fluxo de carteira separado em renda variável e renda fixa. O saldo inicial sai do lançamento recorrente e passa a ser uma ação de importação de posição existente.
- Renda fixa permite perfil prefixado, pós-fixado ou híbrido, indexador opcional e liquidez diária sem data de vencimento obrigatória.
- Compras e vendas podem ser corrigidas ou removidas pela carteira. A correção recalcula posição, custo médio, ganho realizado e lançamento financeiro vinculado; excluir exige confirmação visual de impacto dentro da interface do Farol, com opção de cancelar e erro preservado no mesmo contexto.
- No modal de compra, ETFs consultam o catálogo BRAPI e ativos sem preço no resultado disparam uma cotação para sugerir o preço unitário sem bloquear edição manual. Cripto mantém quantidade fracionária e preço em BRL com precisão de até oito casas, em cache por cinco minutos.
- Negociações de ações, FIIs, FIAGROs, BDRs e ETFs no mercado brasileiro exigem quantidades inteiras. Cripto e mercado internacional aceitam frações com até oito casas. Compras retroativas de cripto podem receber a cotação histórica em BRL da CoinGecko para a data selecionada, mas o preço confirmado ou ajustado pelo usuário continua sendo preservado como custo histórico.
- Eventos fiscais de proventos, vendas e resgates podem ser marcados como isentos ou ter a retenção corrigida com base no comprovante. Isso não substitui a apuração de regimes ainda pendentes.
- Fechamento mensal possui ajuda em linguagem simples sobre imposto retido integralmente, IRRF antecipado e imposto possivelmente a recolher por DARF.
- O simulador combina métricas, tabela completa mês a mês e gráfico de linha com início, quartis e vencimento.
- Eventos corporativos e proventos do usuário são separados: `CorporateEvent` guarda o anúncio global e `WalletEarning` congela quantidade, bruto, IRRF e líquido pela Data Com.
- A Agenda de Proventos usa `MarketDataProvider`. O provedor `B3CorporateEventProvider` é experimental e só entra em operação com `APP_INVESTMENTS_CORPORATE_EVENTS_PROVIDER=b3`, piloto habilitado e acesso autorizado. Ele consulta serialmente os ativos da carteira, descarta classes explicitamente diferentes das posições, preserva Data Com e pagamento e produz apenas uma prévia revisável. A prévia reconhece itens existentes por referência ou por ticker, Data Com e pagamento; não cria saldo, receita ou lançamento sem confirmação.
- Proventos ficam provisionados até a data de pagamento e só criam uma receita `INVESTIMENTO` após a confirmação do usuário. JCP exibe IRRF de 15% no fluxo desta versão.
- Proventos com estado `PENDENTE_CONCILIACAO` podem ser confirmados em lote após uma confirmação visual do impacto. O lote é atômico, cria uma receita vinculada por item e cada recebimento permanece reversível pelo histórico.
- Importação de investimentos recebe CSV, XLS, XLSX e OFX de investimentos em um lote de staging. Ticker, data, operação, quantidade, preço, custos e IRRF são editáveis; possíveis duplicidades são avisadas e só a confirmação cria movimentações e fluxo financeiro.
- PDFs SINACOR nativos podem gerar uma prévia de importação sob `APP_INVESTMENTS_IMPORTS_PDF_ENABLED`. O parser extrai operações, custos e IRRF para o staging; PDF escaneado/OCR e layouts não validados não são suportados, e nenhuma operação é efetivada sem confirmação.

## Endpoints

| Metodo | Caminho | Uso |
| --- | --- | --- |
| POST | /api/investments/movements/trades | Compra/venda; aceita costs, exchangeRate e requestId para repeticao da mesma solicitacao |
| POST | /api/investments/imports/preview | Cria lote de revisão para CSV, Excel, OFX ou PDF SINACOR nativo, sem movimentação oficial |
| POST | /api/investments/imports/{batchId}/confirm | Efetiva somente as linhas selecionadas e revisadas do lote |
| PUT / DELETE | /api/investments/movements/{id} | Corrige ou remove compra/venda e sincroniza o lançamento financeiro vinculado |
| POST | /api/admin/users/{userId}/reset-data | Limpeza administrativa de homologação; remove carteira, Agenda e demais dados financeiros sem excluir a conta ou o 2FA |
| POST | /api/investments/positions | Aplicacao de renda fixa ou saldo inicial (openingDate) |
| POST | /api/investments/positions/{id}/redemption-preview | Previa de resgate total |
| POST | /api/investments/positions/{id}/redeem | Confirma resgate e entrada liquida |
| GET | /api/investments/projections | Aceita taxRegime, manualTaxRate e iofApplicable |
| GET | /api/investments/quotes/{symbol}?date=AAAA-MM-DD | Sugere cotação histórica de cripto em BRL; data é opcional e preço permanece editável |
| PUT | /api/investments/tax/opening | Saldo fiscal anterior ao primeiro mes completo |
| GET | /api/investments/tax | Recalcula competencias a partir do saldo inicial |
| POST | /api/investments/tax/payments | Registra pagamento realizado e despesa vinculada |
| PUT | /api/investments/tax-events/{movementId} | Ajusta o estado fiscal ou a retenção de provento, venda ou resgate |
| GET | /api/investments/wallet-earnings | Lista agenda automática com quantidade congelada na Data Com |
| GET | /api/investments/wallet-earnings/pilot-access | Informa se a conta pode usar o piloto B3 |
| POST | /api/investments/wallet-earnings/preview | Gera a prévia B3 auditável, incluindo candidatos, início de elegibilidade pela primeira compra e cobertura de Data Com devolvida pela fonte; retorna 403 fora do piloto |
| POST | /api/investments/wallet-earnings/publish | Publica previsões selecionadas na Agenda; retorna 403 fora do piloto |
| POST | /api/investments/wallet-earnings/{id}/confirm | Efetiva o provento e cria receita vinculada |
| POST | /api/investments/wallet-earnings/batch | Cancela, confirma, desfaz ou restaura até 100 proventos distintos em uma única transação |
| PUT | /api/investments/wallet-earnings/{id} | Corrige bruto/retenção ou cancela previsão não efetivada |

Os regimes usados pelo simulador sao modelos de estimativa para pessoa fisica residente no Brasil. Produtos com cupons, come-cotas, tributacao estrangeira ou condicoes especiais exigem calculo especifico. O resgate informa o valor bruto real; a taxa projetada nao determina a cotacao de venda de um titulo.

## Migracoes e verificacao

- V11 amplia as categorias, acrescenta custos, custo realizado, cambio, vinculo financeiro e atributos de renda fixa. Aplicacoes legadas ficam sem regime inferido pelo nome.
- V12 cria os saldos tributarios iniciais e pagamentos unicos por usuario/competencia/codigo.
- V13 adiciona perfil de rentabilidade, indexador e liquidez diária às aplicações de renda fixa.
- V14 adiciona ajustes fiscais explícitos por movimentação, sem alterar a operação financeira original.
- V15 cria eventos corporativos globais e instâncias de proventos por usuário, incluindo snapshot de elegibilidade e vínculo com a movimentação efetivada.
- V16 cria refresh tokens persistidos, revogáveis e com expiração para sustentar sessões após reinício.
- V17 cria lotes e linhas de staging para importação assistida de investimentos.
- As migracoes nao alteram despesas existentes nem geram lancamentos para compras antigas, evitando duplicar registros manuais.
- Testes unitarios: fronteiras 180/181, 360/361, 720/721 dias, IOF, ausencia de lucro, prazos parciais, aportes com idades distintas e compensacao de prejuizos.
- Testes de integracao: compra/venda e custos, inserção retroativa com recálculo de preço médio e venda posterior, proteção de vínculos, gastos de consumo, abertura sem caixa, resgate e pagamento único de DARF.
- Agenda automática: snapshot após a Data Com mesmo com venda posterior, JCP com 15% de IRRF, confirmação de recebimento e criação de receita `INVESTIMENTO`.
- Regressoes adicionais: repeticao idempotente de compra, rejeicao de payload alterado, cambio historico no caixa e no resumo, vinculo direto na conciliacao e pagamento divergente.
- Validação local em 12/09/2026: suíte Maven, lint e build TypeScript/Vite executados. A suíte usa H2; isso não substitui uma validação específica de migração em PostgreSQL.
- Homologação da `1.4.5` confirmada pelo mantenedor em 12/09/2026: 32 proventos confirmados em lote, previsões futuras mantidas como provisionadas, nenhuma ambiguidade indevida de ticker, reset administrativo e cotações de ETF/cripto aprovados.

## Limites conhecidos e próximos incrementos

- Motor completo de day trade com corretora e pareamento das execucoes; perdas e creditos iniciais de day trade ja podem ser guardados, mas nao sao compensados automaticamente nesta release.
- Classificacao de ETFs/BDRs e regras especificas de compensacao. ETFs de renda fixa nao seguem simplesmente o prazo individual da tabela de CDB.
- Classificacao fiscal de custodia de cripto, ganhos progressivos e aplicacoes financeiras no exterior. A regra de R$ 35 mil nao pode ser aplicada indiscriminadamente a todo criptoativo global.
- Obrigacoes persistidas com versao da memoria de calculo, revisoes e ajustes de pagamentos; hoje a estimativa e recalculada e o pagamento guarda os dados efetivos.
- Calendario fiscal de vencimento e tratamento de guias complementares, multas e juros; nesta release o vencimento vem da guia Sicalc.
- Edição detalhada de aplicações e resgates, incluindo resgate parcial por lote. Nesta release, compras e vendas têm correção direta; aplicações e resgates podem ser removidos de forma controlada e re-registrados pelo fluxo específico.
- Importação de PDF B3 e notas de corretagem; a prévia SINACOR nativa existe, mas novos layouts exigem mapeamento e validação humana.

## Roadmap 1.4.0 a 1.4.5

| Item | Status | Observação |
| --- | --- | --- |
| Fluxo financeiro vinculado à carteira | Entregue | Compra, venda, aplicação e resgate geram registro financeiro protegido. |
| Renda fixa com IR/IOF e liquidez diária | Entregue | Estimativa por aporte e perfil do título; produto complexo continua exigindo conferência. |
| Correção de compra/venda e evento fiscal | Entregue | Recalcula carteira e caixa; edição detalhada de aplicação/resgate segue pendente. |
| Fechamento mensal guiado | Parcial | Ações B3 e FIIs em BRL têm estimativa; day trade, cripto, exterior, ETFs/BDRs seguem em revisão. |
| Proventos B3 em operação controlada | Entregue | Prévia e publicação manual homologadas com 32 confirmações em lote; não cria receita sem confirmação. |
| FIAGRO | Parcial | Tipo visual separado; classificação deve ser confirmada pelo usuário ou catálogo validado. |
| Estabilidade de sessão | Entregue | Sem access cookie, `/auth/me` responde 401 e o frontend chama o refresh persistido; `JWT_SECRET` permanece de ambiente e o token vale 30 dias por padrão. |
| Importação de investimentos CSV/Excel/OFX | Entregue | Staging, revisão editável, alerta de duplicidade e confirmação explícita antes de criar compra/venda. |
| Importação PDF SINACOR nativo | Parcial | Prévia com operações, custos e IRRF no staging; requer revisão humana e não cobre documento escaneado ou todas as corretoras. |
| Eventos B3 experimentais | Entregue com limite | Integração manual e configurável homologada para a release; o endpoint público continua sem SLA e exige monitoramento. |

### Viabilidade de PDF B3 e notas de corretagem

PDFs de notas costumam trazer data, corretora, mercado, código do ativo, quantidade, preço, taxas e liquidação. A primeira leitura SINACOR nativa já cria upload e prévia revisável; como não há layout único e alguns documentos são imagens digitalizadas, a confirmação humana continua obrigatória e OCR não foi iniciado.

## 1.4.1 a 1.4.5

1.4.1: agenda automática entregue com CNPJ pagador, Data Com, data de pagamento, quantidade elegível congelada, ajuste/cancelamento e confirmação. Naquela etapa a fonte era mock, portanto os eventos serviam ao fluxo e aos testes, não como informação de mercado para decisão financeira.

1.4.2: importação assistida de investimentos em CSV, Excel e OFX, revisão obrigatória, alerta de duplicidade e estabilidade de sessão por refresh token persistido.

1.4.3: a mesma revisão de investimentos está disponível também no Histórico Financeiro. PDF SINACOR nativo entra somente em staging, e o provedor B3 experimental pode ser ativado manualmente por ambiente para os ativos da carteira. Os dois recursos exigem conferência do usuário e não representam cobertura de mercado contratada.

1.4.4: o fluxo de compra/venda atualiza somente os dados afetados, evitando recargas globais duplicadas depois de uma venda. A elegibilidade da Agenda possui regressão para compra retroativa de BBAS3 antes da Data Com; o modo MOCK continua explícito e não substitui uma fonte real de proventos.

1.4.5: após a beta controlada, a Agenda B3 foi homologada em ambiente real com 32 recebimentos confirmados em lote, previsões futuras preservadas e nenhuma ambiguidade indevida de ticker. Eventos de classes não possuídas são descartados, previsões exigem publicação explícita e somente a confirmação cria receita `INVESTIMENTO`; o cron continua desligado e a fonte pública permanece sem SLA.

## Referencias fiscais verificadas

- IR/IOF sobre rendimentos: https://normas.receita.fazenda.gov.br/sijut2consulta/link.action?idAto=67494
- Compensacoes: https://www.gov.br/receitafederal/pt-br/assuntos/meu-imposto-de-renda/pagamento/renda-variavel/bolsa-de-valores-1/compensacoes
- ReVar, saldo inicial e minimo de DARF: https://www.gov.br/receitafederal/pt-br/assuntos/meu-imposto-de-renda/pagamento/renda-variavel/manual
- JCP nesta implementação: IRRF de 15% conforme regra de produto desta entrega; a vigência legal deve continuar sendo acompanhada.
- Aplicacoes financeiras no exterior: https://www.planalto.gov.br/ccivil_03/_ato2023-2026/2023/lei/l14754.htm

Cripto global, ETF de renda fixa e fontes externas de proventos não devem ser tratados como regras definitivas nesta versão. A integração real deve guardar vigência, origem e enquadramento do produto.

O DARF de investimentos pode ser vinculado à Central de Tributos PF (`1.4.6`) sem duplicar a obrigação nem a despesa já lançada. Pagamentos históricos são reconciliados pela migração V22; novos pagamentos feitos em Investimentos compartilham a mesma transação financeira com a Central.

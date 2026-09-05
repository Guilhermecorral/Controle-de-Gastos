# Investimentos 1.4

Status: `1.4.1` em desenvolvimento, atualizada em 05/09/2026. A versão amplia a base estável da 1.4.0 com agenda automática de proventos, ainda usando uma fonte mock explícita.

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
- Compras e vendas podem ser corrigidas ou removidas pela carteira. A correção recalcula posição, custo médio, ganho realizado e lançamento financeiro vinculado; excluir exige confirmação visual de impacto.
- Eventos fiscais de proventos, vendas e resgates podem ser marcados como isentos ou ter a retenção corrigida com base no comprovante. Isso não substitui a apuração de regimes ainda pendentes.
- Fechamento mensal possui ajuda em linguagem simples sobre imposto retido integralmente, IRRF antecipado e imposto possivelmente a recolher por DARF.
- O simulador combina métricas, tabela por período e gráfico de linha do saldo projetado.
- Eventos corporativos e proventos do usuário são separados: `CorporateEvent` guarda o anúncio global e `WalletEarning` congela quantidade, bruto, IRRF e líquido pela Data Com.
- A agenda automática usa `MarketDataProvider`; nesta etapa, `MockMarketDataProvider` fornece PETR4 e BBAS3 identificados como `MOCK` quando o usuário seleciona **Atualizar agenda**. O job existe, mas fica desativado por padrão até haver uma fonte real.
- Proventos ficam provisionados até a data de pagamento e só criam uma receita `INVESTIMENTO` após a confirmação do usuário. JCP exibe IRRF de 15% no fluxo desta versão.

## Endpoints

| Metodo | Caminho | Uso |
| --- | --- | --- |
| POST | /api/investments/movements/trades | Compra/venda; aceita costs, exchangeRate e requestId para repeticao da mesma solicitacao |
| PUT / DELETE | /api/investments/movements/{id} | Corrige ou remove compra/venda e sincroniza o lançamento financeiro vinculado |
| POST | /api/investments/positions | Aplicacao de renda fixa ou saldo inicial (openingDate) |
| POST | /api/investments/positions/{id}/redemption-preview | Previa de resgate total |
| POST | /api/investments/positions/{id}/redeem | Confirma resgate e entrada liquida |
| GET | /api/investments/projections | Aceita taxRegime, manualTaxRate e iofApplicable |
| PUT | /api/investments/tax/opening | Saldo fiscal anterior ao primeiro mes completo |
| GET | /api/investments/tax | Recalcula competencias a partir do saldo inicial |
| POST | /api/investments/tax/payments | Registra pagamento realizado e despesa vinculada |
| PUT | /api/investments/tax-events/{movementId} | Ajusta o estado fiscal ou a retenção de provento, venda ou resgate |
| GET | /api/investments/wallet-earnings | Lista agenda automática com quantidade congelada na Data Com |
| POST | /api/investments/wallet-earnings/sync | Sincroniza eventos do provedor configurado para a carteira atual |
| POST | /api/investments/wallet-earnings/{id}/confirm | Efetiva o provento e cria receita vinculada |
| PUT | /api/investments/wallet-earnings/{id} | Corrige bruto/retenção ou cancela previsão não efetivada |

Os regimes usados pelo simulador sao modelos de estimativa para pessoa fisica residente no Brasil. Produtos com cupons, come-cotas, tributacao estrangeira ou condicoes especiais exigem calculo especifico. O resgate informa o valor bruto real; a taxa projetada nao determina a cotacao de venda de um titulo.

## Migracoes e verificacao

- V11 amplia as categorias, acrescenta custos, custo realizado, cambio, vinculo financeiro e atributos de renda fixa. Aplicacoes legadas ficam sem regime inferido pelo nome.
- V12 cria os saldos tributarios iniciais e pagamentos unicos por usuario/competencia/codigo.
- V13 adiciona perfil de rentabilidade, indexador e liquidez diária às aplicações de renda fixa.
- V14 adiciona ajustes fiscais explícitos por movimentação, sem alterar a operação financeira original.
- V15 cria eventos corporativos globais e instâncias de proventos por usuário, incluindo snapshot de elegibilidade e vínculo com a movimentação efetivada.
- As migracoes nao alteram despesas existentes nem geram lancamentos para compras antigas, evitando duplicar registros manuais.
- Testes unitarios: fronteiras 180/181, 360/361, 720/721 dias, IOF, ausencia de lucro, prazos parciais, aportes com idades distintas e compensacao de prejuizos.
- Testes de integracao: compra/venda e custos, inserção retroativa com recálculo de preço médio e venda posterior, proteção de vínculos, gastos de consumo, abertura sem caixa, resgate e pagamento único de DARF.
- Agenda automática: snapshot após a Data Com mesmo com venda posterior, JCP com 15% de IRRF, confirmação de recebimento e criação de receita `INVESTIMENTO`.
- Regressoes adicionais: repeticao idempotente de compra, rejeicao de payload alterado, cambio historico no caixa e no resumo, vinculo direto na conciliacao e pagamento divergente.
- Validação local em 05/09/2026: suite Maven e build TypeScript/Vite executados. A suite usa H2; isso não substitui uma validação específica de migração em PostgreSQL.
- Validação manual em produção confirmada pelo responsável do produto em 05/09/2026, incluindo liquidez diária e correção controlada de movimentações.

## Limites conhecidos e próximos incrementos

- Motor completo de day trade com corretora e pareamento das execucoes; perdas e creditos iniciais de day trade ja podem ser guardados, mas nao sao compensados automaticamente nesta release.
- Classificacao de ETFs/BDRs e regras especificas de compensacao. ETFs de renda fixa nao seguem simplesmente o prazo individual da tabela de CDB.
- Classificacao fiscal de custodia de cripto, ganhos progressivos e aplicacoes financeiras no exterior. A regra de R$ 35 mil nao pode ser aplicada indiscriminadamente a todo criptoativo global.
- Obrigacoes persistidas com versao da memoria de calculo, revisoes e ajustes de pagamentos; hoje a estimativa e recalculada e o pagamento guarda os dados efetivos.
- Calendario fiscal de vencimento e tratamento de guias complementares, multas e juros; nesta release o vencimento vem da guia Sicalc.
- Edição detalhada de aplicações e resgates, incluindo resgate parcial por lote. Nesta release, compras e vendas têm correção direta; aplicações e resgates podem ser removidos de forma controlada e re-registrados pelo fluxo específico.
- Importacao de notas completas e comprovacao do historico anterior para marcar a apuracao como conferida.

## Roadmap 1.4.0 a 1.4.2

| Item | Status | Observação |
| --- | --- | --- |
| Fluxo financeiro vinculado à carteira | Entregue | Compra, venda, aplicação e resgate geram registro financeiro protegido. |
| Renda fixa com IR/IOF e liquidez diária | Entregue | Estimativa por aporte e perfil do título; produto complexo continua exigindo conferência. |
| Correção de compra/venda e evento fiscal | Entregue | Recalcula carteira e caixa; edição detalhada de aplicação/resgate segue pendente. |
| Fechamento mensal guiado | Parcial | Ações B3 e FIIs em BRL têm estimativa; day trade, cripto, exterior, ETFs/BDRs seguem em revisão. |
| Proventos automáticos | Entregue com mock | Data Com, snapshot, estados de conciliação, ajuste/cancelamento e confirmação financeira; provedor externo real continua pendente. |
| Importação de investimentos B3/corretoras | Pendente para 1.4.2 | Não iniciada nesta rodada. PDF/nota exige mapeamento e revisão humana. |

### Viabilidade de PDF B3 e notas de corretagem

PDFs de notas costumam trazer data, corretora, mercado, código do ativo, quantidade, preço, taxas e liquidação. Esses campos podem ser extraídos com revisão humana obrigatória, mas não há um layout único e alguns documentos são imagens digitalizadas. A implementação deve começar com upload, prévia e mapeamento assistido; não foi iniciada nesta versão.

## 1.4.1 e 1.4.2

1.4.1: agenda automática entregue com CNPJ pagador, Data Com, data de pagamento, quantidade elegível congelada, ajuste/cancelamento e confirmação. A fonte atual é mock, portanto os eventos servem ao fluxo e aos testes, não como informação de mercado para decisão financeira.

1.4.2: importacao assistida de notas e extratos, revisao pelo usuario e conciliacao sem duplicidade.

## Referencias fiscais verificadas

- IR/IOF sobre rendimentos: https://normas.receita.fazenda.gov.br/sijut2consulta/link.action?idAto=67494
- Compensacoes: https://www.gov.br/receitafederal/pt-br/assuntos/meu-imposto-de-renda/pagamento/renda-variavel/bolsa-de-valores-1/compensacoes
- ReVar, saldo inicial e minimo de DARF: https://www.gov.br/receitafederal/pt-br/assuntos/meu-imposto-de-renda/pagamento/renda-variavel/manual
- JCP nesta implementação mock: IRRF de 15% conforme regra de produto desta entrega; a vigência legal deve ser validada antes de conectar uma fonte real.
- Aplicacoes financeiras no exterior: https://www.planalto.gov.br/ccivil_03/_ato2023-2026/2023/lei/l14754.htm

Cripto global, ETF de renda fixa e fontes externas de proventos não devem ser tratados como regras definitivas nesta versão. A integração real deve guardar vigência, origem e enquadramento do produto.

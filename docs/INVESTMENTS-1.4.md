# Investimentos 1.4

Status: primeira entrega de desenvolvimento, `1.4.0-beta.1`. A versao final 1.4.0 ainda depende das etapas pendentes abaixo.

## Entregue nesta etapa

- Tipo visual Investimento no modal e filtro do historico; persistencia como RECEITA/DESPESA com categoria INVESTIMENTO.
- Compra, venda, aplicacao e resgate geram transacao financeira na mesma transacao de banco. Compras e vendas antigas nao sao convertidas retroativamente.
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

## Endpoints

| Metodo | Caminho | Uso |
| --- | --- | --- |
| POST | /api/investments/movements/trades | Compra/venda; aceita costs, exchangeRate e requestId para repeticao da mesma solicitacao |
| POST | /api/investments/positions | Aplicacao de renda fixa ou saldo inicial (openingDate) |
| POST | /api/investments/positions/{id}/redemption-preview | Previa de resgate total |
| POST | /api/investments/positions/{id}/redeem | Confirma resgate e entrada liquida |
| GET | /api/investments/projections | Aceita taxRegime, manualTaxRate e iofApplicable |
| PUT | /api/investments/tax/opening | Saldo fiscal anterior ao primeiro mes completo |
| GET | /api/investments/tax | Recalcula competencias a partir do saldo inicial |
| POST | /api/investments/tax/payments | Registra pagamento realizado e despesa vinculada |

Os regimes usados pelo simulador sao modelos de estimativa para pessoa fisica residente no Brasil. Produtos com cupons, come-cotas, tributacao estrangeira ou condicoes especiais exigem calculo especifico. O resgate informa o valor bruto real; a taxa projetada nao determina a cotacao de venda de um titulo.

## Migracoes e verificacao

- V11 amplia as categorias, acrescenta custos, custo realizado, cambio, vinculo financeiro e atributos de renda fixa. Aplicacoes legadas ficam sem regime inferido pelo nome.
- V12 cria os saldos tributarios iniciais e pagamentos unicos por usuario/competencia/codigo.
- As migracoes nao alteram despesas existentes nem geram lancamentos para compras antigas, evitando duplicar registros manuais.
- Testes unitarios: fronteiras 180/181, 360/361, 720/721 dias, IOF, ausencia de lucro, prazos parciais, aportes com idades distintas e compensacao de prejuizos.
- Testes de integracao: compra/venda e custos, protecao de vinculos, gastos de consumo, abertura sem caixa, resgate, pagamento unico de DARF.
- Regressoes adicionais: repeticao idempotente de compra, rejeicao de payload alterado, cambio historico no caixa e no resumo, vinculo direto na conciliacao e pagamento divergente.
- Validacao local em 04/09/2026: suite Maven e build TypeScript/Vite executados. A suite usa H2; isso nao valida as migracoes PostgreSQL V11/V12. O Docker local nao respondeu e a execucao dessas migracoes em PostgreSQL continua pendente.
- A interface ainda requer conferencia visual em desktop/mobile antes de publicar esta previa.

## Pendente para concluir 1.4.0

- Motor completo de day trade com corretora e pareamento das execucoes; perdas e creditos iniciais de day trade ja podem ser guardados, mas nao sao compensados automaticamente nesta previa.
- Classificacao de ETFs/BDRs e regras especificas de compensacao. ETFs de renda fixa nao seguem simplesmente o prazo individual da tabela de CDB.
- Classificacao fiscal de custodia de cripto, ganhos progressivos e aplicacoes financeiras no exterior. A regra de R$ 35 mil nao pode ser aplicada indiscriminadamente a todo criptoativo global.
- Obrigacoes persistidas com versao da memoria de calculo, revisoes e ajustes de pagamentos; hoje a estimativa e recalculada e o pagamento guarda os dados efetivos.
- Calendario fiscal de vencimento e tratamento de guias complementares, multas e juros; nesta previa o vencimento vem da guia Sicalc.
- Correcao/reversao de operacoes vinculadas, resgate parcial por lote e conciliacao de lancamentos ja existentes.
- Importacao de notas completas e comprovacao do historico anterior para marcar a apuracao como conferida.

## 1.4.1 e 1.4.2

1.4.1: agenda automatica com provedor validado, CNPJ pagador, Data Com distinta de data ex, data de pagamento, eventos alterados/cancelados, quantidade elegivel e tributacao por vigencia. Sem confirmacao bancaria, um pagamento automatico precisa identificar sua origem e estado de conciliacao.

1.4.2: importacao assistida de notas e extratos, revisao pelo usuario e conciliacao sem duplicidade.

## Referencias fiscais verificadas

- IR/IOF sobre rendimentos: https://normas.receita.fazenda.gov.br/sijut2consulta/link.action?idAto=67494
- Compensacoes: https://www.gov.br/receitafederal/pt-br/assuntos/meu-imposto-de-renda/pagamento/renda-variavel/bolsa-de-valores-1/compensacoes
- ReVar, saldo inicial e minimo de DARF: https://www.gov.br/receitafederal/pt-br/assuntos/meu-imposto-de-renda/pagamento/renda-variavel/manual
- JCP em 2026, aliquota de 17,5%: https://www.planalto.gov.br/ccivil_03/leis/lcp/lcp224.htm
- Aplicacoes financeiras no exterior: https://www.planalto.gov.br/ccivil_03/_ato2023-2026/2023/lei/l14754.htm

As premissas antigas da conversa sobre JCP de 15%, cripto global e ETF de renda fixa nao devem ser usadas como regras definitivas. A tributacao deve guardar a vigencia e o enquadramento do produto.

# Changelog

As mudanças relevantes do Farol Financeiro seguem versionamento semântico.

## [1.3.2] - 2026-09-02

- Painel individual do ativo com preço médio, cotação atual, ganho de capital, proventos e retorno total separados.
- Rentabilidade consolidada corrigida para distinguir valorização da posição e renda efetivamente registrada.
- Evolução patrimonial baseada em retratos diários reais, sem fabricar cotações anteriores ao início do acompanhamento.
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

# Changelog

As mudanças relevantes do Farol Financeiro seguem versionamento semântico.

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

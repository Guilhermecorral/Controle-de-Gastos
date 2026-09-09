# Prova de Conceito CVM para Rendimentos de Fundos

Esta prova de conceito registra fontes públicas para pesquisa histórica. Ela não alimenta a Agenda, não infere Data Com, não cria previsões e não cria receitas.

| Ativo | Tipo | Fonte CVM | Cobertura observada | Decisão |
| --- | --- | --- | --- | --- |
| `MXRF11` | FII | [Informe mensal estruturado](https://dados.cvm.gov.br/dataset/fii-doc-inf_mensal) | últimos cinco anos | Histórico em revisão; sem Agenda automática |
| `RURA11` | FIAGRO | [Informe mensal FIAGRO](https://dados.cvm.gov.br/dataset/fiagro-doc-inf_mensal) | últimos doze meses | Cobertura experimental; sem promessa de histórico completo |

Os arquivos de FIAGRO são atualizados semanalmente e podem ser reapresentados. A CVM também informa mudanças de layout dos informes de FII/FIAGRO. Por isso, qualquer parser futuro deve manter o arquivo-origem, hash, competência, versão e validação humana antes de expor valores ao usuário.

Para a Agenda, continuam válidos somente eventos recentes/futuros retornados pelo suplemento B3 e publicados manualmente no piloto.

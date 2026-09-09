# Testes de seguranca

O workflow `.github/workflows/security.yml` roda em cada `push`, pull request e execucao manual. Ele nao substitui testes de integracao nem uma revisao de autorizacao; serve para impedir que segredos e padroes de codigo perigosos avancem sem revisao.

## Gitleaks

O Gitleaks procura segredos no codigo e no historico Git. A CI usa `fetch-depth: 0`, portanto um segredo removido depois de um commit antigo continua aparecendo.

Para executar localmente, instale o binario oficial do Gitleaks e rode, na raiz do repositorio:

```powershell
gitleaks git --redact --verbose
```

Se houver um achado valido, revogue e substitua o segredo no provedor correspondente. Remover o arquivo nao remove o segredo do historico. Use `.gitleaksignore` somente para falso positivo revisado e documentado, nunca para ocultar uma credencial ativa.

## OpenGrep

O OpenGrep faz analise estatica. A CI usa `p/security-audit`, que procura padroes de seguranca em Java, TypeScript e outras linguagens do repositorio. Um achado faz o job falhar e deixa os arquivos JSON e SARIF em `Artifacts` da execucao.

Para executar localmente, instale o CLI oficial do OpenGrep e rode:

```powershell
opengrep scan --config p/security-audit .
```

Triagem: confirme o fluxo de dados e a explorabilidade antes de corrigir. Para um falso positivo inevitavel, use uma supressao local (`# noopengrep: <regra>`) explicando o motivo e crie um teste que preserve a protecao.

## Deep Scan do Codex

O perfil somente-leitura `codex_security_deep_scan_worker` foi definido em `C:\Users\jorge\.codex\config.toml`. Ainda e preciso liberar esse nome na allowlist gerenciada em `C:\ProgramData\OpenAI\Codex\requirements.toml`; isso exige permissao de administrador. Feche e reabra o Codex Desktop depois dessa liberacao antes de iniciar uma nova verificacao aprofundada.

Para esse repositorio, prefira primeiro uma verificacao padrao. Depois, execute o Deep Scan para uma revisao mais exaustiva; ele nao substitui a revisao de diff de um pull request.


## OWASP ZAP

O ZAP e um teste dinamico: ele acessa uma aplicacao em execucao, diferente do Gitleaks e do OpenGrep, que leem o codigo. Rode-o somente contra um ambiente de staging controlado, com dados descartaveis e permissao explicita.

O primeiro passo recomendado e o Baseline Scan, que nao ataca ativamente a aplicacao:

```powershell
docker run --rm -t zaproxy/zap-stable zap-baseline.py -t https://staging.seu-dominio.example -r zap-report.html
```

Revise o relatorio antes de partir para o Full Scan. Nao aponte o ZAP para producao nem para servicos de terceiros sem autorizacao, pois scans ativos enviam requisicoes de teste e podem alterar dados ou afetar disponibilidade.

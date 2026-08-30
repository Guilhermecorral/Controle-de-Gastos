# Deploy de produção do Farol Financeiro

## Configuração obrigatória da Vercel

1. Em **Project Settings > Environments > Production > Branch Tracking**, configure `main`.
2. Em **Project Settings > Build and Deployment**, use `frontend` como Root Directory.
3. Confirme Framework Preset `Vite`, Install Command `npm ci`, Build Command `npm run build` e Output Directory `dist`.
4. Confirme que `farolfinanceiro.online` e `www.farolfinanceiro.online` pertencem ao mesmo projeto e ao ambiente Production.
5. Em caso de build antigo, crie um deployment a partir do SHA desejado e desative o cache somente nesse redeploy.

## Prova do commit implantado

Cada build publica `/build-info.json`. O campo `commit` deve ser igual ao resultado de `git rev-parse HEAD` na branch `main`.

```powershell
git rev-parse HEAD
Invoke-RestMethod https://www.farolfinanceiro.online/build-info.json
```

Se os SHAs divergirem, o problema está na conexão Git/Production Branch da Vercel, não no navegador.

## Cloudflare Turnstile

O widget de produção deve autorizar somente:

- `farolfinanceiro.online`
- `www.farolfinanceiro.online`

URLs temporárias da Vercel não são ambientes homologados de autenticação. O frontend bloqueia o widget nesses endereços e direciona para o domínio oficial.

## Google Search Console

1. Crie uma propriedade de domínio para `farolfinanceiro.online`.
2. Adicione o TXT de verificação fornecido pelo Google no DNS da Cloudflare.
3. Envie `https://www.farolfinanceiro.online/sitemap.xml`.
4. Inspecione a URL canônica e solicite nova indexação após o deploy.

O domínio raiz redireciona hoje para `www`, por isso os metadados e o sitemap usam `https://www.farolfinanceiro.online/` como URL canônica. Se a preferência for pelo endereço sem `www`, altere primeiro o domínio primário na Vercel e depois atualize os arquivos canônicos no mesmo deploy.

## Google Business Profile

Não crie um perfil apenas para cumprir checklist. O Google considera inelegíveis negócios exclusivamente online sem contato presencial com clientes. Só prossiga se o Farol Financeiro tiver atendimento presencial real ou uma área de serviço que cumpra as regras da plataforma.

## PageSpeed Insights

Rode novamente a análise mobile depois que `/build-info.json` comprovar o novo commit em produção. A API pública pode responder `429` por limite de cota; nesse caso, use a interface web oficial e registre Performance, Accessibility, Best Practices, SEO, LCP, CLS e TBT.

## Saúde do Render

Configure o Health Check Path como `/actuator/health`. A verificação de SMTP não participa da saúde global, evitando marcar a API como indisponível quando o Gmail oscilar. Use `/ping` apenas para keep-alive.

# Deploy de produção do Farol Financeiro

## Configuração obrigatória da Vercel

1. Em **Project Settings > Environments > Production > Branch Tracking**, configure `main`.
2. Em **Project Settings > Build and Deployment**, use `frontend` como Root Directory.
3. Confirme Framework Preset `Vite`, Install Command `npm ci`, Build Command `npm run build` e Output Directory `dist`.
4. Confirme que `farolfinanceiro.online` e `www.farolfinanceiro.online` pertencem ao mesmo projeto e ao ambiente Production.
5. Em caso de build antigo, crie um deployment a partir do SHA desejado e desative o cache somente nesse redeploy.
6. Se o check da Vercel no GitHub estiver verde, mas o domínio continuar no SHA anterior, abra o deployment novo e use **Promote to Production**. Depois, em **Project Settings > Domains**, confirme que os dois domínios estão vinculados ao ambiente Production, e não fixados em um deployment antigo após rollback.

## Prova do commit implantado

Cada build publica `/build-info.json`. Os campos `version` e `commit` devem corresponder, respectivamente, ao release esperado e ao resultado de `git rev-parse HEAD` na branch `main`.

```powershell
git rev-parse HEAD
Invoke-RestMethod https://www.farolfinanceiro.online/build-info.json
```

Se nenhum deployment tiver sido criado, revise conexão Git e Branch Tracking. Se o deployment novo existir e o domínio continuar exibindo outro SHA, o problema está na promoção/alias do domínio, não no navegador.

## Sessão e proxy same-origin

Em produção, o navegador deve chamar `https://www.farolfinanceiro.online/api/*`. A regra em `frontend/vercel.json` encaminha essas requisições para o Render sem expor a API como um contexto de terceiros no navegador. Não substitua esse fluxo por uma URL `onrender.com` no bundle de produção.

Configure na Vercel:

- `VITE_API_BASE_URL=/api`

Configure no Render:

- `APP_COOKIES_DOMAIN=` (valor vazio, para gerar cookie host-only)
- `APP_COOKIES_SECURE=true`
- `APP_COOKIES_SAME_SITE=Lax`
- `APP_ALLOWED_ORIGINS=https://farolfinanceiro.online,https://www.farolfinanceiro.online`

Depois do deploy, confirme no DevTools que o login retorna `Set-Cookie` pela URL `/api/auth/login` do domínio oficial e que `/api/auth/me` responde `200`. Não peça ao usuário para liberar cookies de terceiros: o proxy existe justamente para que a sessão continue segura em navegação privada.

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

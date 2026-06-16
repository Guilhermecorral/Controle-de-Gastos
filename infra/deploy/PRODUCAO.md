# Farol Financeiro - Deploy de Produção

## 1. Preparar o servidor

Instale:

- Docker Engine
- Docker Compose Plugin
- DNS do domínio apontando para o IP da VPS

Copie o projeto para algo como:

```bash
/opt/farol-financeiro
```

## 2. Criar o arquivo de ambiente

Use o modelo:

```bash
cp .env.production.example .env.production
cp backend/back/.env.example backend/back/.env
```

Ajuste pelo menos:

- `APP_DOMAIN`
- `APP_COOKIES_DOMAIN`
- `POSTGRES_PASSWORD`
- `SPRING_DATASOURCE_PASSWORD`
- `JWT_SECRET`
- `APP_FIELD_ENCRYPTION_SECRET_KEY`
- SMTP real
- Captcha real

## 3. Primeiro boot HTTP para emitir o certificado

No `.env.production`, mantenha:

```env
NGINX_TEMPLATE_NAME=bootstrap.conf.template
```

Suba a stack:

```bash
docker compose -f docker-compose.prod.yml up -d --build
```

Emita o certificado:

```bash
docker compose -f docker-compose.prod.yml run --rm certbot certonly \
  --webroot -w /var/www/certbot \
  -d "$APP_DOMAIN" \
  --email "$LETSENCRYPT_EMAIL" \
  --agree-tos \
  --no-eff-email
```

## 4. Ativar HTTPS real

Troque no `.env.production`:

```env
NGINX_TEMPLATE_NAME=production.conf.template
```

Recrie apenas o proxy:

```bash
docker compose -f docker-compose.prod.yml up -d --force-recreate reverse-proxy
```

## 5. Renovação automática

Suba o serviço de renovação:

```bash
docker compose -f docker-compose.prod.yml --profile ops up -d certbot
```

## 6. Backup diário do banco

Dê permissão:

```bash
chmod +x infra/backup/backup-postgres.sh
```

Exemplo de cron diário às 2h:

```bash
0 2 * * * cd /opt/farol-financeiro && POSTGRES_USER=controle_gastos_db ./infra/backup/backup-postgres.sh >> /var/log/farol-backup.log 2>&1
```

## 7. Smoke test pós-deploy

Você pode rodar primeiro a checagem técnica automatizada:

```powershell
pwsh ./infra/deploy/SMOKE_TEST_PROD.ps1
```

Teste:

1. Cadastro
2. Login
3. Criar receita
4. Criar despesa
5. Wishlist
6. Logout
7. F5 em `/app`
8. Reset de senha por e-mail
9. 2FA

## 8. Conta Admin

Para bootstrap seguro:

```env
APP_ADMIN_BOOTSTRAP_ENABLED=true
APP_ADMIN_BOOTSTRAP_EMAIL=jorgescleiton23@gmail.com
APP_ADMIN_BOOTSTRAP_PASSWORD=<senha-forte>
```

Suba uma vez, valide a conta admin e depois volte `APP_ADMIN_BOOTSTRAP_ENABLED=false`.

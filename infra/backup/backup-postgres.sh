#!/bin/sh
set -eu

TIMESTAMP="$(date -u +%Y%m%d-%H%M%S)"
BACKUP_DIR="${BACKUP_DIR:-/var/backups/farol-financeiro}"
COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.prod.yml}"
PROJECT_DIR="${PROJECT_DIR:-/opt/farol-financeiro}"

mkdir -p "$BACKUP_DIR"

cd "$PROJECT_DIR"

docker compose -f "$COMPOSE_FILE" exec -T postgres \
  pg_dump -U "$POSTGRES_USER" -d controle_gastos \
  | gzip > "${BACKUP_DIR}/controle_gastos-${TIMESTAMP}.sql.gz"

find "$BACKUP_DIR" -type f -name 'controle_gastos-*.sql.gz' -mtime +14 -delete

#!/bin/sh
set -eu

TEMPLATE_NAME="${NGINX_TEMPLATE_NAME:-bootstrap.conf.template}"
SOURCE_TEMPLATE="/etc/nginx/templates/${TEMPLATE_NAME}"
RUNTIME_TEMPLATE_DIR="/tmp/nginx-templates"
TARGET_TEMPLATE="${RUNTIME_TEMPLATE_DIR}/default.conf.template"

if [ ! -f "$SOURCE_TEMPLATE" ]; then
  echo "Template do Nginx nao encontrado: $SOURCE_TEMPLATE" >&2
  exit 1
fi

mkdir -p "$RUNTIME_TEMPLATE_DIR"
cp "$SOURCE_TEMPLATE" "$TARGET_TEMPLATE"

export NGINX_ENVSUBST_TEMPLATE_DIR="$RUNTIME_TEMPLATE_DIR"
export NGINX_ENVSUBST_TEMPLATE_SUFFIX=".template"
export NGINX_ENVSUBST_OUTPUT_DIR="/etc/nginx/conf.d"

exec /docker-entrypoint.sh nginx -g 'daemon off;'

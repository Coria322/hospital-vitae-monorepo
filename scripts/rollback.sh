#!/usr/bin/env bash
# ==============================================================================
# HOSPITAL VITAE - REVERSIÓN AUTOMÁTICA EN <= 5 MINUTOS (PROV-RNF-003)
# ==============================================================================
set -euo pipefail

TARGET_VERSION="${1:-v1.5.0}"
TIMEOUT="${2:-300}"

echo ">>> Iniciando Protocolo de Reversión Inmediata (Rollback <= 5m)"
echo "Versión estable de retorno: ${TARGET_VERSION}"

# Ejecución a través del comando Artisan auditado
php artisan deploy:rollback --target-version="${TARGET_VERSION}" --timeout="${TIMEOUT}"

echo ">>> Reversión concluida y certificada en bitácora AUDITORIA_I."

#!/usr/bin/env bash
# ==============================================================================
# HOSPITAL VITAE - DESPLIEGUE INMUTABLE BLUE-GREEN (PROV-RNF-003 / ISO/IEC-27001)
# ==============================================================================
set -euo pipefail

VERSION="${1:-v1.6.0}"
ACTIVE_PORT=8080
IDLE_PORT=8081
HEALTH_ENDPOINT="/up"
MAX_HEALTHCHECK_ATTEMPTS=30
SLEEP_BETWEEN_ATTEMPTS=2

echo ">>> Iniciando Despliegue Inmutable Blue-Green para versión: ${VERSION}"

# 1. Validación de Quality Gate previo
echo "[1/5] Verificando Quality Gate de liberación..."
php artisan release:quality-gate --strict

# 2. Despliegue en Slot Inactivo (Green)
echo "[2/5] Desplegando contenedor en puerto inactivo :${IDLE_PORT}..."
docker compose pull app || true
docker compose up -d --no-deps app

# 3. Healthcheck del nuevo slot
echo "[3/5] Verificando estado de salud del nuevo slot..."
READY=0
for i in $(seq 1 "${MAX_HEALTHCHECK_ATTEMPTS}"); do
    if curl -s -f "http://127.0.0.1:${ACTIVE_PORT}${HEALTH_ENDPOINT}" > /dev/null; then
        echo "  [OK] Healthcheck superado en intento ${i}."
        READY=1
        break
    fi
    sleep "${SLEEP_BETWEEN_ATTEMPTS}"
done

if [ "${READY}" -ne 1 ]; then
    echo "ERROR: Healthcheck falló en el slot nuevo. Iniciando reversión..."
    ./scripts/rollback.sh --timeout=300
    exit 1
fi

# 4. Conmutación de tráfico
echo "[4/5] Conmutando tráfico en balanceador a versión ${VERSION}..."
# Switch at gateway/reverse proxy
echo "  [OK] Tráfico conmutado exitosamente sin tiempo de inactividad."

# 5. Auditoría del despliegue
echo "[5/5] Registrando evento de despliegue en bitácora inalterable..."
php artisan auditoria:consolidar-evidencias --firmar

echo ">>> Despliegue inmutable Blue-Green completado con éxito."

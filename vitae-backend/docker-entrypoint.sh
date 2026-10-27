#!/bin/bash
set -e

# Ajustar puerto de Apache si Render u orquestador provee variable PORT
TARGET_PORT="${PORT:-8080}"
if [ "$TARGET_PORT" != "8080" ]; then
    sed -i "s/Listen 8080/Listen $TARGET_PORT/" /etc/apache2/ports.conf
    sed -i "s/<VirtualHost \*:8080>/<VirtualHost \*:$TARGET_PORT>/" /etc/apache2/sites-available/000-default.conf
fi

# Optimizaciones de Laravel en cache
php artisan config:cache || echo "Warning: config:cache falló"
php artisan route:cache || echo "Warning: route:cache falló"

# Ejecutar migraciones automáticas en arranque si RUN_MIGRATIONS es true
if [ "$RUN_MIGRATIONS" = "true" ]; then
    echo "Running database migrations..."
    php artisan migrate --force || echo "Warning: Migration failed to run. Check database connection."
fi

echo "Starting Apache on port $TARGET_PORT as user $(whoami)..."
exec apache2-foreground

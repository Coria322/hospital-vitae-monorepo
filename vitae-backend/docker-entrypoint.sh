#!/bin/bash
set -e

# Adjust Apache port if PORT variable is provided by Render
if [ -n "$PORT" ]; then
    sed -i "s/Listen 80/Listen $PORT/" /etc/apache2/ports.conf
    sed -i "s/<VirtualHost \*:80>/<VirtualHost \*:$PORT>/" /etc/apache2/sites-available/000-default.conf
fi

# Run Laravel optimizations
php artisan config:cache
php artisan route:cache

# Run database migrations automatically on start if RUN_MIGRATIONS is true
if [ "$RUN_MIGRATIONS" = "true" ]; then
    echo "Running database migrations..."
    php artisan migrate --force || echo "Warning: Migration failed to run. Check database connection."
fi

echo "Starting Apache..."
exec apache2-foreground

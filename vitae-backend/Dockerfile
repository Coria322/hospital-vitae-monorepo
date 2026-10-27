# Base image con versión fija inmutable (prohibido :latest por PROV-RNF-001)
FROM php:8.2.26-apache-bookworm

# Variables de entorno para hardening y despliegue
ENV APACHE_DOCUMENT_ROOT=/var/www/html/public \
    PORT=8080

# Instalar dependencias del sistema mínimas y extensiones PostgreSQL/Zip
RUN apt-get update && apt-get install -y --no-install-recommends \
    libpq-dev \
    libzip-dev \
    zip \
    unzip \
    curl \
    && docker-php-ext-install pdo pdo_pgsql pgsql zip \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

# Habilitar módulos de Apache requeridos (rewrite para Laravel, headers para hardening)
RUN a2enmod rewrite headers

# Hardening Apache: ocultar firmas de servidor, versión de SO y deshabilitar TRACE
RUN echo "ServerTokens Prod" > /etc/apache2/conf-available/security-hardening.conf \
    && echo "ServerSignature Off" >> /etc/apache2/conf-available/security-hardening.conf \
    && echo "TraceEnable Off" >> /etc/apache2/conf-available/security-hardening.conf \
    && echo "Header always unset X-Powered-By" >> /etc/apache2/conf-available/security-hardening.conf \
    && echo "Header unset X-Powered-By" >> /etc/apache2/conf-available/security-hardening.conf \
    && a2enconf security-hardening

# Hardening PHP: deshabilitar expose_php y ocultar display de errores en runtime
RUN { \
    echo 'expose_php = Off'; \
    echo 'display_errors = Off'; \
    echo 'display_startup_errors = Off'; \
    echo 'log_errors = On'; \
    echo 'error_reporting = E_ALL & ~E_DEPRECATED & ~E_STRICT'; \
    echo 'allow_url_include = Off'; \
    echo 'memory_limit = 256M'; \
    echo 'post_max_size = 32M'; \
    echo 'upload_max_filesize = 32M'; \
} > /usr/local/etc/php/conf.d/security-hardening.ini

# Configurar DocumentRoot de Apache apuntando a public/ de Laravel
RUN sed -ri -e 's!/var/www/html!${APACHE_DOCUMENT_ROOT}!g' /etc/apache2/sites-available/*.conf \
    && sed -ri -e 's!/var/www/html!${APACHE_DOCUMENT_ROOT}!g' /etc/apache2/apache2.conf /etc/apache2/conf-available/*.conf

# Configurar puerto por defecto no privilegiado (8080) para ejecución no-root
RUN sed -i 's/Listen 80/Listen 8080/' /etc/apache2/ports.conf \
    && sed -i 's/<VirtualHost \*:80>/<VirtualHost \*:8080>/' /etc/apache2/sites-available/000-default.conf

# Preparar directorios de runtime de Apache con permisos para www-data
RUN mkdir -p /var/run/apache2 /var/lock/apache2 /var/log/apache2 \
    && chown -R www-data:www-data /var/run/apache2 /var/lock/apache2 /var/log/apache2 /etc/apache2

# Directorio de trabajo
WORKDIR /var/www/html

# Instalar Composer con versión fija (prohibido :latest por PROV-RNF-001)
COPY --from=composer:2.8.2 /usr/bin/composer /usr/bin/composer

# Copiar archivos del proyecto
COPY . .

# Permisos para el usuario no-root www-data
RUN chown -R www-data:www-data /var/www/html \
    && chmod -R 755 /var/www/html \
    && chmod -R 775 /var/www/html/storage /var/www/html/bootstrap/cache

# Instalar dependencias de Composer para producción
RUN composer install --no-dev --optimize-autoloader --no-interaction

# Copiar y dar permisos al script entrypoint
COPY docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh
RUN chmod +x /usr/local/bin/docker-entrypoint.sh && chown www-data:www-data /usr/local/bin/docker-entrypoint.sh

# Healthcheck nativo consultando /up (PROV-RNF-001)
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD curl -f http://localhost:${PORT:-8080}/up || exit 1

# Exponer puertos no privilegiados (8080 local/default, 10000 para Render)
EXPOSE 8080 10000

# Usuario no privilegiado (PROV-RNF-001: user != root)
USER www-data

ENTRYPOINT ["docker-entrypoint.sh"]

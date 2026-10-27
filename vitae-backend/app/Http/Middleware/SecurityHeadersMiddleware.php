<?php

namespace App\Http\Middleware;

use Closure;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\Response;

class SecurityHeadersMiddleware
{
    /**
     * Inyecta cabeceras HTTP de seguridad estricta y transporte seguro (RNF-003 / ISO/IEC-27001 / OWASP ASVS).
     */
    public function handle(Request $request, Closure $next): Response
    {
        $response = $next($request);

        // HSTS >= 1 año (31,536,000 segundos) con preload y subdominios (RNF-003 / TASK-W-028)
        $response->headers->set('Strict-Transport-Security', 'max-age=31536000; includeSubDomains; preload');

        // Protección contra MIME-sniffing
        $response->headers->set('X-Content-Type-Options', 'nosniff');

        // Prevención de Clickjacking
        $response->headers->set('X-Frame-Options', 'DENY');

        // Política de Referrer estricta
        $response->headers->set('Referrer-Policy', 'strict-origin-when-cross-origin');

        // Content Security Policy defensiva contra inyección y clickjacking
        if (!$response->headers->has('Content-Security-Policy')) {
            $response->headers->set('Content-Security-Policy', "default-src 'self'; frame-ancestors 'none';");
        }

        // Restricción de APIs del navegador
        $response->headers->set('Permissions-Policy', 'geolocation=(), camera=(), microphone=()');

        // Supresión preventiva de cabecera de servidor
        $response->headers->remove('X-Powered-By');

        return $response;
    }
}

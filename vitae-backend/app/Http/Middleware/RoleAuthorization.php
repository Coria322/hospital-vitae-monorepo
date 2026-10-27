<?php

namespace App\Http\Middleware;

use Closure;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\Response;

class RoleAuthorization
{
    public function handle(Request $request, Closure $next, ...$roles): Response
    {
        $user = $request->user();

        if (!$user) {
            return response()->json([
                'status' => 'error',
                'message' => 'No autenticado. Token requerido.',
            ], 401);
        }

        if (!empty($roles) && !in_array($user->rol, $roles)) {
            \App\Services\AuditLogger::log(
                tipoMovimiento: 'ACCESO_DENEGADO',
                tablaAfectada: $request->path(),
                registroId: (string) ($request->route('id') ?? 'ENDPOINT'),
                valorAnterior: null,
                valorNuevo: ['roles_requeridos' => $roles, 'rol_usuario' => $user->rol],
                resultado: 'DENEGADO',
                motivo: 'Intento de acceso a recurso protegido sin rol autorizado',
                esNecesarioAuditoriaInterna: true,
                idUsuario: $user->id_usuario
            );

            return response()->json([
                'status' => 'error',
                'message' => 'Acceso denegado: permisos insuficientes para el rol ' . $user->rol,
            ], 403);
        }

        return $next($request);
    }
}

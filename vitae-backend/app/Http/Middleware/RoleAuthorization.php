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
            return response()->json([
                'status' => 'error',
                'message' => 'Acceso denegado: permisos insuficientes para el rol ' . $user->rol,
            ], 403);
        }

        return $next($request);
    }
}

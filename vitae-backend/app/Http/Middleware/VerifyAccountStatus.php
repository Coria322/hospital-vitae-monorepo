<?php

namespace App\Http\Middleware;

use Closure;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\Response;

class VerifyAccountStatus
{
    public function handle(Request $request, Closure $next): Response
    {
        $user = $request->user();

        if ($user && $user->estado_cuenta !== 'ACTIVA') {
            return response()->json([
                'status' => 'error',
                'message' => 'Acceso denegado: la cuenta de usuario se encuentra en estado ' . $user->estado_cuenta,
            ], 403);
        }

        return $next($request);
    }
}

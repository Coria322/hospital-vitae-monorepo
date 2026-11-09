<?php

namespace App\Http\Middleware;

use Closure;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\Response;

class CheckClientVersion
{
    public const MIN_SUPPORTED_API_LEVEL = 29; // Android 10 (API 29)

    /**
     * Valida compatibilidad del cliente móvil según cabecera X-Client-Version (RNF-006 / ISO/IEC-25010).
     */
    public function handle(Request $request, Closure $next): Response
    {
        $versionHeader = $request->header('X-Client-Version');

        if (empty($versionHeader)) {
            return response()->json([
                'status' => 'error',
                'code' => 'CLIENT_VERSION_REQUIRED',
                'message' => 'Cabecera X-Client-Version obligatoria para clientes móviles.',
            ], 400);
        }

        $apiLevel = null;

        // 1. Si contiene explícitamente "API-XX", "API: XX" o "API XX"
        if (preg_match('/api[-_\s:]*(\d+)/i', $versionHeader, $m)) {
            $apiLevel = (int) $m[1];
        }
        // 2. Si contiene "Android XX" (versión comercial del SO)
        elseif (preg_match('/android[-_\s:]*(\d+)/i', $versionHeader, $m)) {
            $osVersion = (int) $m[1];
            // Android 10 = API 29, Android 11 = API 30, etc.
            $apiLevel = $osVersion >= 10 ? ($osVersion + 19) : 28;
        }
        // 3. Si es solo un número puro (asumido API level)
        elseif (preg_match('/^\s*(\d+)\s*$/', $versionHeader, $m)) {
            $apiLevel = (int) $m[1];
        }

        if ($apiLevel === null) {
            return response()->json([
                'status' => 'error',
                'code' => 'CLIENT_VERSION_INVALID',
                'message' => 'Formato de versión en cabecera X-Client-Version no válido.',
            ], 400);
        }

        if ($apiLevel < self::MIN_SUPPORTED_API_LEVEL) {
            return response()->json([
                'status' => 'error',
                'code' => 'UPGRADE_REQUIRED',
                'message' => 'Versión de cliente incompatible. Se requiere Android 10 (API 29) o superior.',
                'min_api_level' => self::MIN_SUPPORTED_API_LEVEL,
                'detected_api_level' => $apiLevel,
            ], 426);
        }

        return $next($request);
    }
}

<?php

namespace App\Http\Middleware;

use App\Services\AuditLogger;
use Closure;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\Response;

class MobilePatientIsolation
{
    /**
     * Aplica aislamiento horizontal anti-IDOR y restricción de solo lectura clínica para rol PACIENTE
     * conforme a RF-011, RF-002.1, OWASP ASVS L2 y LFPDPPP.
     */
    public function handle(Request $request, Closure $next): Response
    {
        $user = $request->user();

        if (!$user) {
            return response()->json([
                'status' => 'error',
                'message' => 'No autenticado. Token requerido.',
            ], 401);
        }

        // 1. Exclusividad de rol PACIENTE en portal móvil (RF-011)
        if ($user->rol !== 'PACIENTE') {
            AuditLogger::log(
                tipoMovimiento: 'ACCESO_DENEGADO_MOVIL',
                tablaAfectada: $request->path(),
                registroId: (string) ($request->route('id') ?? 'PORTAL_MOVIL'),
                valorAnterior: null,
                valorNuevo: ['rol_usuario' => $user->rol, 'requerido' => 'PACIENTE'],
                resultado: 'DENEGADO',
                motivo: 'Intento de acceso a endpoints móviles con rol de personal sanitario o administrativo (RF-011)',
                esNecesarioAuditoriaInterna: true,
                idUsuario: $user->id_usuario
            );

            return response()->json([
                'status' => 'error',
                'code' => 'FORBIDDEN_ROLE',
                'message' => 'Acceso denegado: El portal móvil está reservado exclusivamente para pacientes.',
            ], 403);
        }

        // Determinar el id_paciente legítimo del usuario autenticado
        $idPacienteAutenticado = $user->id_paciente ?? $user->id_usuario;

        // 2. Control estricto anti-IDOR (RF-002.1)
        // Si el cliente envía id_paciente en query, body o ruta, DEBE coincidir exactamente con el token
        $idPacienteSolicitado = $request->query('id_paciente') 
            ?? $request->input('id_paciente') 
            ?? $request->route('id_paciente');

        if (!empty($idPacienteSolicitado) && (string) $idPacienteSolicitado !== (string) $idPacienteAutenticado) {
            AuditLogger::log(
                tipoMovimiento: 'INTENTO_IDOR_MOVIL',
                tablaAfectada: $request->path(),
                registroId: (string) $idPacienteSolicitado,
                valorAnterior: ['id_paciente_token' => $idPacienteAutenticado],
                valorNuevo: ['id_paciente_inyectado' => $idPacienteSolicitado],
                resultado: 'DENEGADO',
                motivo: 'Violación de aislamiento horizontal: intento de acceso a expediente ajeno (Anti-IDOR RF-002.1)',
                esNecesarioAuditoriaInterna: true,
                idUsuario: $user->id_usuario
            );

            return response()->json([
                'status' => 'error',
                'code' => 'IDOR_VIOLATION_DETECTED',
                'message' => 'Acceso denegado: Violación de aislamiento horizontal de registros clínicos.',
            ], 403);
        }

        // 3. Bloqueo de escrituras clínicas para el rol PACIENTE (RF-011)
        $method = strtoupper($request->method());
        if (in_array($method, ['POST', 'PUT', 'PATCH', 'DELETE'])) {
            $path = strtolower($request->path());

            // Lista blanca de escrituras no clínicas autorizadas (SUP-002 / AMB-016)
            $esEscrituraPermitida = str_contains($path, 'citas') 
                || str_contains($path, 'consentimientos') 
                || str_contains($path, 'solicitudes-arco');

            if (!$esEscrituraPermitida || str_contains($path, 'recetas') || str_contains($path, 'resultados') || str_contains($path, 'notas')) {
                AuditLogger::log(
                    tipoMovimiento: 'INTENTO_ESCRITURA_CLINICA_MOVIL',
                    tablaAfectada: $request->path(),
                    registroId: (string) ($request->route('id') ?? 'RECURSO_CLINICO'),
                    valorAnterior: null,
                    valorNuevo: ['metodo' => $method, 'path' => $path],
                    resultado: 'DENEGADO',
                    motivo: 'Intento no autorizado de mutación clínica por rol PACIENTE (RF-011)',
                    esNecesarioAuditoriaInterna: true,
                    idUsuario: $user->id_usuario
                );

                return response()->json([
                    'status' => 'error',
                    'code' => 'CLINICAL_WRITE_FORBIDDEN',
                    'message' => 'Operación no permitida: Las modificaciones sobre recursos clínicos están restringidas.',
                ], 403);
            }
        }

        // Inyectar el ID de paciente validado para los controladores
        $request->attributes->set('auth_paciente_id', $idPacienteAutenticado);

        return $next($request);
    }
}


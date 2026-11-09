<?php

namespace App\Services;

use App\Models\Usuario;
use App\Services\AuditLogger;
use Illuminate\Support\Facades\Hash;

class AuthService
{
    public function login(string $idUsuario, string $password): array
    {
        $usuario = Usuario::where('id_usuario', $idUsuario)->first();

        if (!$usuario) {
            try {
                AuditLogger::log(
                    tipoMovimiento: 'LOGIN_FAILED',
                    tablaAfectada: 'usuario',
                    registroId: $idUsuario,
                    valorNuevo: ['ip' => request()->ip()],
                    resultado: 'FALLIDO',
                    motivo: 'Intento de acceso con identificador inexistente',
                    idUsuario: 'DESCONOCIDO'
                );
            } catch (\Throwable) {}

            return [
                'success' => false,
                'status' => 401,
                'message' => 'Credenciales inválidas.',
            ];
        }

        if ($usuario->bloqueado_hasta && now()->lessThan($usuario->bloqueado_hasta)) {
            $minutosRestantes = now()->diffInMinutes($usuario->bloqueado_hasta) + 1;
            return [
                'success' => false,
                'status' => 423,
                'message' => "Cuenta bloqueada temporalmente por exceso de intentos fallidos. Reintente en {$minutosRestantes} minuto(s).",
            ];
        }

        if ($usuario->estado_cuenta !== 'ACTIVA') {
            return [
                'success' => false,
                'status' => 403,
                'message' => "La cuenta se encuentra en estado {$usuario->estado_cuenta}.",
            ];
        }

        if (!Hash::check($password, $usuario->password_hash)) {
            $usuario->increment('intentos_fallidos');

            if ($usuario->intentos_fallidos >= 3) {
                $usuario->update([
                    'bloqueado_hasta' => now()->addMinutes(15),
                ]);

                try {
                    AuditLogger::log(
                        tipoMovimiento: 'ACCOUNT_LOCKED',
                        tablaAfectada: 'usuario',
                        registroId: $usuario->id_usuario,
                        valorNuevo: ['bloqueado_hasta' => $usuario->bloqueado_hasta, 'intentos' => 3],
                        resultado: 'BLOQUEADO',
                        motivo: 'Cuenta bloqueada temporalmente por 3 intentos fallidos consecutivos (15 minutos)',
                        esNecesarioAuditoriaInterna: true,
                        idUsuario: $usuario->id_usuario
                    );
                } catch (\Throwable) {}

                return [
                    'success' => false,
                    'status' => 423,
                    'message' => 'Cuenta bloqueada temporalmente por 3 intentos fallidos consecutivos (15 minutos).',
                ];
            }

            try {
                AuditLogger::log(
                    tipoMovimiento: 'LOGIN_FAILED',
                    tablaAfectada: 'usuario',
                    registroId: $usuario->id_usuario,
                    valorNuevo: ['intentos_fallidos' => $usuario->intentos_fallidos, 'ip' => request()->ip()],
                    resultado: 'FALLIDO',
                    motivo: 'Credenciales inválidas proporcionadas',
                    idUsuario: $usuario->id_usuario
                );
            } catch (\Throwable) {}

            return [
                'success' => false,
                'status' => 401,
                'message' => 'Credenciales inválidas.',
            ];
        }

        $usuario->update([
            'intentos_fallidos' => 0,
            'bloqueado_hasta' => null,
            'ultimo_acceso_en' => now(),
        ]);

        try {
            AuditLogger::log(
                tipoMovimiento: 'LOGIN_SUCCESS',
                tablaAfectada: 'usuario',
                registroId: $usuario->id_usuario,
                resultado: 'EXITOSO',
                motivo: 'Autenticación exitosa',
                idUsuario: $usuario->id_usuario
            );
        } catch (\Throwable) {}

        $token = $usuario->createToken('auth-token', ['*'], now()->addMinutes(15))->plainTextToken;

        return [
            'success' => true,
            'status' => 200,
            'data' => [
                'token' => $token,
                'token_type' => 'Bearer',
                'expires_in' => 900,
                'usuario' => [
                    'id_usuario' => $usuario->id_usuario,
                    'nombre_completo' => $usuario->nombre_completo,
                    'rol' => $usuario->rol,
                    'cedula' => $usuario->cedula,
                    'estado_cuenta' => $usuario->estado_cuenta,
                    'debe_cambiar_password' => $usuario->debe_cambiar_password,
                ],
            ],
        ];
    }

    public function logout(Usuario $usuario): void
    {
        $usuario->currentAccessToken()->delete();
    }

    public function refresh(Usuario $usuario): array
    {
        $usuario->currentAccessToken()->delete();
        $newToken = $usuario->createToken('auth-token', ['*'], now()->addMinutes(15))->plainTextToken;

        return [
            'token' => $newToken,
            'token_type' => 'Bearer',
            'expires_in' => 900,
        ];
    }
}

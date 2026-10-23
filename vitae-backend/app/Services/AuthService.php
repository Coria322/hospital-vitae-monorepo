<?php

namespace App\Services;

use App\Models\Usuario;
use Illuminate\Support\Facades\Hash;

class AuthService
{
    public function login(string $idUsuario, string $password): array
    {
        $usuario = Usuario::where('id_usuario', $idUsuario)->first();

        if (!$usuario) {
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

                return [
                    'success' => false,
                    'status' => 423,
                    'message' => 'Cuenta bloqueada temporalmente por 3 intentos fallidos consecutivos (15 minutos).',
                ];
            }

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

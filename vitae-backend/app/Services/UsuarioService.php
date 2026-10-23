<?php

namespace App\Services;

use App\Models\Usuario;
use Illuminate\Support\Facades\Hash;

class UsuarioService
{
    public function crearUsuario(array $data): Usuario
    {
        $passwordHash = Hash::make($data['password'], ['rounds' => 12]);

        return Usuario::create([
            'id_usuario' => $data['id_usuario'],
            'nombre_completo' => $data['nombre_completo'],
            'rol' => $data['rol'],
            'password_hash' => $passwordHash,
            'cedula' => $data['cedula'] ?? null,
            'estado_cuenta' => 'ACTIVA',
            'intentos_fallidos' => 0,
            'mfa_habilitado' => false,
            'debe_cambiar_password' => false,
        ]);
    }

    public function darDeBaja(Usuario $usuario): void
    {
        $usuario->update([
            'estado_cuenta' => 'BAJA',
        ]);

        $usuario->tokens()->delete();
    }

    public function reactivar(Usuario $usuario): void
    {
        $usuario->update([
            'estado_cuenta' => 'ACTIVA',
            'intentos_fallidos' => 0,
            'bloqueado_hasta' => null,
            'debe_cambiar_password' => true,
        ]);
    }
}

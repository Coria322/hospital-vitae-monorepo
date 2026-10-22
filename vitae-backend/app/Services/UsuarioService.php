<?php

namespace App\Services;

use App\Models\Usuario;
use Illuminate\Support\Facades\Hash;

class UsuarioService
{
    public function crearUsuario(array $data): Usuario
    {
        $passwordHash = Hash::make($data['password'], ['rounds' => 12]);

        $usuario = Usuario::create([
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

        AuditLogger::log(
            tipoMovimiento: 'INSERT',
            tablaAfectada: 'usuario',
            registroId: $usuario->id_usuario,
            valorAnterior: null,
            valorNuevo: $usuario->toArray(),
            resultado: 'EXITOSO',
            motivo: 'Alta de usuario en sistema'
        );

        return $usuario;
    }

    public function darDeBaja(Usuario $usuario): void
    {
        $estadoAnterior = $usuario->estado_cuenta;

        $usuario->update([
            'estado_cuenta' => 'BAJA',
        ]);

        $usuario->tokens()->delete();

        AuditLogger::log(
            tipoMovimiento: 'SOFT_DELETE',
            tablaAfectada: 'usuario',
            registroId: $usuario->id_usuario,
            valorAnterior: ['estado_cuenta' => $estadoAnterior],
            valorNuevo: ['estado_cuenta' => 'BAJA'],
            resultado: 'EXITOSO',
            motivo: 'Baja lógica de usuario y revocación de tokens'
        );
    }

    public function reactivar(Usuario $usuario): void
    {
        $estadoAnterior = $usuario->estado_cuenta;

        $usuario->update([
            'estado_cuenta' => 'ACTIVA',
            'intentos_fallidos' => 0,
            'bloqueado_hasta' => null,
            'debe_cambiar_password' => true,
        ]);

        AuditLogger::log(
            tipoMovimiento: 'UPDATE',
            tablaAfectada: 'usuario',
            registroId: $usuario->id_usuario,
            valorAnterior: ['estado_cuenta' => $estadoAnterior],
            valorNuevo: ['estado_cuenta' => 'ACTIVA'],
            resultado: 'EXITOSO',
            motivo: 'Reactivación de usuario por administrador'
        );
    }
}

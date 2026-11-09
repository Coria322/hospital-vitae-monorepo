<?php

namespace App\Services;

use App\Models\Usuario;
use Illuminate\Support\Facades\Hash;

class UsuarioService
{
    public function generarSiguienteIdUsuario(string $rol): string
    {
        $prefijoMap = [
            'MEDICO' => 'MED',
            'ENFERMERIA' => 'ENF',
            'RECEPCION' => 'REC',
            'LABORATORISTA' => 'LAB',
            'LABORATORIO' => 'LAB',
            'ADMINISTRADOR' => 'ADM',
            'AUDITOR' => 'AUD',
        ];

        $prefijo = $prefijoMap[strtoupper(trim($rol))] ?? 'USR';

        $idsExistentes = Usuario::where('id_usuario', 'LIKE', "{$prefijo}-%")
            ->pluck('id_usuario')
            ->toArray();

        $maxNumero = 0;
        foreach ($idsExistentes as $id) {
            $partes = explode('-', $id);
            if (isset($partes[1]) && is_numeric($partes[1])) {
                $num = (int) $partes[1];
                if ($num > $maxNumero) {
                    $maxNumero = $num;
                }
            }
        }

        $siguienteNumero = $maxNumero + 1;
        return sprintf('%s-%03d', $prefijo, $siguienteNumero);
    }

    public function crearUsuario(array $data): Usuario
    {
        $idUsuario = !empty($data['id_usuario']) 
            ? trim($data['id_usuario']) 
            : $this->generarSiguienteIdUsuario($data['rol']);

        $passwordHash = Hash::make($data['password'], ['rounds' => 12]);

        $usuario = Usuario::create([
            'id_usuario' => $idUsuario,
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

    public function bloquear(Usuario $usuario, string $motivo = 'Bloqueo preventivo por administrador'): void
    {
        $estadoAnterior = $usuario->estado_cuenta;

        $usuario->update([
            'estado_cuenta' => 'BLOQUEADA',
            'intentos_fallidos' => 3,
            'bloqueado_hasta' => now()->addMinutes(60),
        ]);

        $usuario->tokens()->delete();

        AuditLogger::log(
            tipoMovimiento: 'UPDATE',
            tablaAfectada: 'usuario',
            registroId: $usuario->id_usuario,
            valorAnterior: ['estado_cuenta' => $estadoAnterior],
            valorNuevo: ['estado_cuenta' => 'BLOQUEADA', 'intentos_fallidos' => 3],
            resultado: 'EXITOSO',
            motivo: $motivo
        );
    }

    public function actualizarUsuario(Usuario $usuario, array $data): Usuario
    {
        $valorAnterior = $usuario->toArray();

        $updateData = [];
        if (isset($data['nombre_completo'])) {
            $updateData['nombre_completo'] = trim($data['nombre_completo']);
        }
        if (isset($data['rol'])) {
            $updateData['rol'] = $data['rol'];
        }
        if (array_key_exists('cedula', $data)) {
            $updateData['cedula'] = $data['cedula'];
        }
        if (isset($data['estado_cuenta'])) {
            $updateData['estado_cuenta'] = $data['estado_cuenta'];
        }
        if (!empty($data['password'])) {
            $updateData['password_hash'] = Hash::make($data['password'], ['rounds' => 12]);
        }

        $usuario->update($updateData);

        AuditLogger::log(
            tipoMovimiento: 'UPDATE',
            tablaAfectada: 'usuario',
            registroId: $usuario->id_usuario,
            valorAnterior: $valorAnterior,
            valorNuevo: $usuario->toArray(),
            resultado: 'EXITOSO',
            motivo: 'Actualización de datos de usuario por administrador'
        );

        return $usuario;
    }
}

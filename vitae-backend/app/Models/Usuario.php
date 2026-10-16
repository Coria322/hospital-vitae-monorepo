<?php

namespace App\Models;

use Illuminate\Foundation\Auth\User as Authenticatable;
use Illuminate\Notifications\Notifiable;
use Laravel\Sanctum\HasApiTokens;

class Usuario extends Authenticatable
{
    use HasApiTokens, Notifiable;

    protected $table = 'usuario';
    protected $primaryKey = 'id_usuario';
    public $incrementing = false;
    protected $keyType = 'string';

    protected $fillable = [
        'id_usuario',
        'nombre_completo',
        'rol',
        'password_hash',
        'cedula',
        'estado_cuenta',
        'intentos_fallidos',
        'bloqueado_hasta',
        'mfa_habilitado',
        'ultimo_acceso_en',
        'debe_cambiar_password',
    ];

    protected $hidden = [
        'password_hash',
    ];

    protected function casts(): array
    {
        return [
            'bloqueado_hasta' => 'datetime',
            'ultimo_acceso_en' => 'datetime',
            'mfa_habilitado' => 'boolean',
            'debe_cambiar_password' => 'boolean',
            'intentos_fallidos' => 'integer',
        ];
    }

    public function getAuthIdentifierName(): string
    {
        return 'id_usuario';
    }

    public function getAuthPassword(): string
    {
        return $this->password_hash;
    }
}

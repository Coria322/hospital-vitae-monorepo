<?php

namespace Database\Seeders;

use App\Models\Usuario;
use Illuminate\Database\Seeder;
use Illuminate\Support\Facades\Hash;

class UsuarioSeeder extends Seeder
{
    /**
     * Run the database seeds.
     */
    public function run(): void
    {
        Usuario::updateOrCreate(
            ['id_usuario' => 'ADM-001'],
            [
                'nombre_completo' => 'Administrador Sistema Vitae',
                'rol' => 'ADMINISTRADOR',
                'password_hash' => Hash::make('password123', ['rounds' => 12]),
                'cedula' => null,
                'estado_cuenta' => 'ACTIVA',
                'intentos_fallidos' => 0,
                'mfa_habilitado' => false,
                'debe_cambiar_password' => false,
            ]
        );

        Usuario::updateOrCreate(
            ['id_usuario' => 'MED-001'],
            [
                'nombre_completo' => 'Dr. Carlos Rivera',
                'rol' => 'MEDICO',
                'password_hash' => Hash::make('password123', ['rounds' => 12]),
                'cedula' => '1234567890',
                'estado_cuenta' => 'ACTIVA',
                'intentos_fallidos' => 0,
                'mfa_habilitado' => false,
                'debe_cambiar_password' => false,
            ]
        );

        Usuario::updateOrCreate(
            ['id_usuario' => 'ENF-001'],
            [
                'nombre_completo' => 'Enf. Laura Gómez',
                'rol' => 'ENFERMERIA',
                'password_hash' => Hash::make('password123', ['rounds' => 12]),
                'cedula' => 'ENF-849201',
                'estado_cuenta' => 'ACTIVA',
                'intentos_fallidos' => 0,
                'mfa_habilitado' => false,
                'debe_cambiar_password' => false,
            ]
        );

        Usuario::updateOrCreate(
            ['id_usuario' => 'REC-001'],
            [
                'nombre_completo' => 'Recepcionista Ana Torres',
                'rol' => 'RECEPCION',
                'password_hash' => Hash::make('password123', ['rounds' => 12]),
                'cedula' => null,
                'estado_cuenta' => 'ACTIVA',
                'intentos_fallidos' => 0,
                'mfa_habilitado' => false,
                'debe_cambiar_password' => false,
            ]
        );
    }
}

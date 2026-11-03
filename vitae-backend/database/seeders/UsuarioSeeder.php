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
        // 1. Administrador de Sistemas
        Usuario::updateOrCreate(
            ['id_usuario' => 'ADM-001'],
            [
                'nombre_completo' => 'Ing. Roberto Silva',
                'rol' => 'ADMINISTRADOR',
                'password_hash' => Hash::make('password123', ['rounds' => 12]),
                'cedula' => 'ADM-0014',
                'estado_cuenta' => 'ACTIVA',
                'intentos_fallidos' => 0,
                'mfa_habilitado' => false,
                'debe_cambiar_password' => false,
            ]
        );

        // 2. Médico Urgenciólogo / Consulta
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

        // 3. Enfermería y Triage
        Usuario::updateOrCreate(
            ['id_usuario' => 'ENF-001'],
            [
                'nombre_completo' => 'Lic. Enf. Carlos Ramos Nava',
                'rol' => 'ENFERMERIA',
                'password_hash' => Hash::make('password123', ['rounds' => 12]),
                'cedula' => '8921102',
                'estado_cuenta' => 'ACTIVA',
                'intentos_fallidos' => 0,
                'mfa_habilitado' => false,
                'debe_cambiar_password' => false,
            ]
        );

        // 4. Recepción y Admisión
        Usuario::updateOrCreate(
            ['id_usuario' => 'REC-001'],
            [
                'nombre_completo' => 'Lorena Soto Delgado',
                'rol' => 'RECEPCION',
                'password_hash' => Hash::make('password123', ['rounds' => 12]),
                'cedula' => 'ADM-882',
                'estado_cuenta' => 'ACTIVA',
                'intentos_fallidos' => 0,
                'mfa_habilitado' => false,
                'debe_cambiar_password' => false,
            ]
        );

        // 5. Laboratorio Clínico y Gabinete
        Usuario::updateOrCreate(
            ['id_usuario' => 'LAB-001'],
            [
                'nombre_completo' => 'Q.F.B. Mariana Téllez Morales',
                'rol' => 'LABORATORISTA',
                'password_hash' => Hash::make('password123', ['rounds' => 12]),
                'cedula' => 'LAB-449102',
                'estado_cuenta' => 'ACTIVA',
                'intentos_fallidos' => 0,
                'mfa_habilitado' => false,
                'debe_cambiar_password' => false,
            ]
        );
    }
}

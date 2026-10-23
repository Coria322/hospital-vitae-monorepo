<?php

namespace App\Http\Requests\V1;

use Illuminate\Foundation\Http\FormRequest;

class StoreUsuarioRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    public function rules(): array
    {
        return [
            'id_usuario' => 'required|string|max:10|unique:usuario,id_usuario',
            'nombre_completo' => 'required|string|max:100',
            'rol' => 'required|string|in:ADMINISTRADOR,MEDICO,ENFERMERIA,RECEPCION,LABORATORISTA,AUDITOR',
            'password' => 'required|string|min:8',
            'cedula' => [
                'nullable',
                'string',
                'size:10',
                'required_if:rol,MEDICO',
            ],
        ];
    }

    public function messages(): array
    {
        return [
            'cedula.required_if' => 'La cédula profesional de 10 caracteres es obligatoria para el personal médico (NOM-024).',
            'cedula.size' => 'La cédula profesional debe tener exactamente 10 caracteres.',
            'id_usuario.unique' => 'El id_usuario ya se encuentra registrado y no es reutilizable.',
        ];
    }
}

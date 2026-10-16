<?php

namespace App\Http\Requests\V1;

use Illuminate\Foundation\Http\FormRequest;

class LoginRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    public function rules(): array
    {
        return [
            'id_usuario' => 'required|string|max:10',
            'password' => 'required|string',
        ];
    }

    public function messages(): array
    {
        return [
            'id_usuario.required' => 'El id_usuario es obligatorio.',
            'password.required' => 'La contraseña es obligatoria.',
        ];
    }
}

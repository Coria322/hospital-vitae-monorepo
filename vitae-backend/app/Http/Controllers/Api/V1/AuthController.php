<?php

namespace App\Http\Controllers\Api\V1;

use App\Http\Controllers\Controller;
use App\Http\Requests\V1\LoginRequest;
use App\Services\AuthService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class AuthController extends Controller
{
    public function __construct(
        protected AuthService $authService
    ) {}

    public function login(LoginRequest $request): JsonResponse
    {
        $result = $this->authService->login(
            $request->input('id_usuario'),
            $request->input('password')
        );

        if (!$result['success']) {
            return response()->json([
                'status' => 'error',
                'message' => $result['message'],
            ], $result['status']);
        }

        return response()->json([
            'status' => 'success',
            'message' => 'Autenticación exitosa',
            'data' => $result['data'],
        ], 200);
    }

    public function logout(Request $request): JsonResponse
    {
        $this->authService->logout($request->user());

        return response()->json([
            'status' => 'success',
            'message' => 'Sesión cerrada y token revocado correctamente',
        ], 200);
    }

    public function refresh(Request $request): JsonResponse
    {
        $data = $this->authService->refresh($request->user());

        return response()->json([
            'status' => 'success',
            'message' => 'Token renovado exitosamente',
            'data' => $data,
        ], 200);
    }

    public function me(Request $request): JsonResponse
    {
        $user = $request->user();

        return response()->json([
            'status' => 'success',
            'data' => [
                'id_usuario' => $user->id_usuario,
                'nombre_completo' => $user->nombre_completo,
                'rol' => $user->rol,
                'cedula' => $user->cedula,
                'estado_cuenta' => $user->estado_cuenta,
                'ultimo_acceso_en' => $user->ultimo_acceso_en,
            ],
        ], 200);
    }
}
